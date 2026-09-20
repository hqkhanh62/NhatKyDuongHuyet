package com.example.nhatkyduonghuyet.scan

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.GlucoseRiskLevel
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Locale

@ExperimentalCoroutinesApi
class ScanProAiTest {

    @Mock
    private lateinit var repository: LogRepository

    private lateinit var pipeline: ScanAutoImportPipeline

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(repository.getLogsByDate(any())).thenReturn(flowOf(emptyList()))
        pipeline = ScanAutoImportPipeline(repository)
    }

    // ---------- SessionClassifier ----------

    @Test
    fun `session boundaries match diary sessions`() {
        assertEquals("Sáng", SessionClassifier.fromHour(0))
        assertEquals("Sáng", SessionClassifier.fromHour(9))
        assertEquals("Trưa", SessionClassifier.fromHour(10))
        assertEquals("Trưa", SessionClassifier.fromHour(15))
        assertEquals("Chiều", SessionClassifier.fromHour(16))
        assertEquals("Chiều", SessionClassifier.fromHour(19))
        assertEquals("Tối", SessionClassifier.fromHour(20))
        assertEquals("Tối", SessionClassifier.fromHour(23))
    }

    @Test
    fun `session from scanned time string`() {
        assertEquals("Sáng", SessionClassifier.fromTime("08:32"))
        assertEquals("Trưa", SessionClassifier.fromTime("13:05"))
        assertEquals("Chiều", SessionClassifier.fromTime("17:20"))
        assertEquals("Tối", SessionClassifier.fromTime("21:00"))
    }

    // ---------- GlucoseRiskEvaluator ----------

    @Test
    fun `risk levels follow policy thresholds`() {
        assertEquals(GlucoseRiskLevel.LOW, GlucoseRiskEvaluator.evaluate(3.9f).level)
        assertTrue(GlucoseRiskEvaluator.evaluate(3.9f).isDanger)

        assertEquals(GlucoseRiskLevel.NORMAL, GlucoseRiskEvaluator.evaluate(4.0f).level)
        assertEquals(GlucoseRiskLevel.NORMAL, GlucoseRiskEvaluator.evaluate(10.0f).level)
        assertFalse(GlucoseRiskEvaluator.evaluate(6.2f).isDanger)

        assertEquals(GlucoseRiskLevel.HIGH, GlucoseRiskEvaluator.evaluate(10.1f).level)
        assertFalse(GlucoseRiskEvaluator.evaluate(10.1f).isDanger)

        assertEquals(GlucoseRiskLevel.VERY_HIGH, GlucoseRiskEvaluator.evaluate(13.1f).level)
        assertTrue(GlucoseRiskEvaluator.evaluate(13.1f).isDanger)
    }

    // ---------- Hba1cEstimator ----------

    @Test
    fun `hba1c uses dashboard formula`() {
        assertEquals((6.0 + 2.59) / 1.59, Hba1cEstimator.fromSingleReading(6.0f), 1e-9)
        assertEquals("5.4%", Hba1cEstimator.format(Hba1cEstimator.fromSingleReading(6.0f)))
    }

    // ---------- ScanStabilityTracker ----------

    @Test
    fun `stability requires repeated matches within tolerance`() {
        val tracker = ScanStabilityTracker()
        assertNull(tracker.add(6.1f))
        assertNull(tracker.add(6.2f))
        assertEquals(6.2f, tracker.add(6.15f))
    }

    @Test
    fun `stability rejects flickering frames`() {
        val tracker = ScanStabilityTracker()
        assertNull(tracker.add(6.1f))
        assertNull(tracker.add(9.9f))
        assertNull(tracker.add(5.0f))
        assertNull(tracker.add(7.7f))
    }

    @Test
    fun `stability reset clears window`() {
        val tracker = ScanStabilityTracker()
        tracker.add(6.1f)
        tracker.add(6.1f)
        tracker.reset()
        assertEquals(0, tracker.sampleCount())
        assertNull(tracker.add(6.1f))
    }

    // ---------- ScanAutoImportPipeline.buildDraft ----------

    private fun fixedNow(): java.util.Date =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("2026-09-20 14:05")!!

    @Test
    fun `draft prioritizes meter date and time`() {
        val draft = pipeline.buildDraft(
            ScannedGlucoseResult(value = 6.2f, date = "2026-08-20", time = "08:32"),
            fixedNow()
        )
        assertNotNull(draft)
        assertEquals("2026-08-20", draft!!.date)
        assertEquals("08:32", draft.time)
        assertEquals(ScanDateSource.METER, draft.dateSource)
        assertEquals(ScanTimeSource.METER, draft.timeSource)
        assertEquals("Sáng", draft.session)
        assertEquals(GlucoseRiskLevel.NORMAL, draft.risk.level)
    }

    @Test
    fun `draft falls back to system date and time`() {
        val draft = pipeline.buildDraft(ScannedGlucoseResult(value = 7.5f), fixedNow())
        assertNotNull(draft)
        assertEquals("2026-09-20", draft!!.date)
        assertEquals("14:05", draft.time)
        assertEquals(ScanDateSource.SYSTEM, draft.dateSource)
        assertEquals(ScanTimeSource.SYSTEM, draft.timeSource)
        assertEquals("Trưa", draft.session)
    }

    @Test
    fun `draft classifies session from scanned hour`() {
        assertEquals(
            "Chiều",
            pipeline.buildDraft(
                ScannedGlucoseResult(value = 6.0f, time = "17:20"),
                fixedNow()
            )!!.session
        )
        assertEquals(
            "Tối",
            pipeline.buildDraft(
                ScannedGlucoseResult(value = 6.0f, time = "21:00"),
                fixedNow()
            )!!.session
        )
    }

    @Test
    fun `draft auto-cleans noise outside safe range`() {
        assertNull(pipeline.buildDraft(ScannedGlucoseResult(value = 1.5f), fixedNow()))
        assertNull(pipeline.buildDraft(ScannedGlucoseResult(value = 35.0f), fixedNow()))
        assertNull(pipeline.buildDraft(ScannedGlucoseResult(value = Float.NaN), fixedNow()))
    }

    @Test
    fun `draft falls back when meter date is invalid`() {
        val draft = pipeline.buildDraft(
            ScannedGlucoseResult(value = 6.0f, date = "not-a-date", time = "09:15"),
            fixedNow()
        )
        assertNotNull(draft)
        assertEquals("2026-09-20", draft!!.date)
        assertEquals(ScanDateSource.SYSTEM, draft.dateSource)
        assertEquals("09:15", draft.time)
        assertEquals(ScanTimeSource.METER, draft.timeSource)
    }

    @Test
    fun `draft repairs yyyy-dd-MM swap`() {
        val draft = pipeline.buildDraft(
            ScannedGlucoseResult(value = 6.0f, date = "2026-20-08", time = "09:15"),
            fixedNow()
        )
        assertEquals("2026-08-20", draft!!.date)
        assertEquals(ScanDateSource.METER, draft.dateSource)
    }

    // ---------- ScanAutoImportPipeline.confirmAndSave ----------

    @Test
    fun `confirm saves new session row`() = runTest {
        val draft = pipeline.buildDraft(
            ScannedGlucoseResult(value = 6.2f, date = "2026-09-20", time = "08:32"),
            fixedNow()
        )!!

        val result = pipeline.confirmAndSave(draft)

        assertTrue(result is ScanSaveResult.Saved)
        assertFalse((result as ScanSaveResult.Saved).isUpdate)
        val captor = argumentCaptor<LogEntry>()
        verify(repository).insertLog(captor.capture())
        assertEquals("2026-09-20", captor.firstValue.date)
        assertEquals("Sáng", captor.firstValue.session)
        assertEquals("08:32", captor.firstValue.time)
        assertEquals(6.2, captor.firstValue.bgBefore!!, 1e-9)
    }

    @Test
    fun `confirm fills empty slot of existing session`() = runTest {
        val existing = LogEntry(
            id = 7L, date = "2026-09-20", session = "Sáng",
            time = "07:00", bgBefore = 5.5
        )
        whenever(repository.getLogsByDate("2026-09-20")).thenReturn(flowOf(listOf(existing)))

        val draft = pipeline.buildDraft(
            ScannedGlucoseResult(value = 6.8f, date = "2026-09-20", time = "09:10"),
            fixedNow()
        )!!

        val result = pipeline.confirmAndSave(draft)

        assertTrue(result is ScanSaveResult.Saved)
        assertTrue((result as ScanSaveResult.Saved).isUpdate)
        val captor = argumentCaptor<LogEntry>()
        verify(repository).insertLog(captor.capture())
        assertEquals(5.5, captor.firstValue.bgBefore!!, 1e-9)
        assertEquals(6.8, captor.firstValue.bgAfter!!, 1e-9)
        assertEquals("09:10", captor.firstValue.time)
    }

    @Test
    fun `confirm skips duplicate scan`() = runTest {
        val existing = LogEntry(
            id = 7L, date = "2026-09-20", session = "Sáng",
            time = "08:32", bgBefore = 6.2
        )
        whenever(repository.getLogsByDate("2026-09-20")).thenReturn(flowOf(listOf(existing)))

        val draft = pipeline.buildDraft(
            ScannedGlucoseResult(value = 6.2f, date = "2026-09-20", time = "08:32"),
            fixedNow()
        )!!

        val result = pipeline.confirmAndSave(draft)

        assertTrue(result is ScanSaveResult.Duplicate)
        verify(repository, never()).insertLog(any())
    }

    @Test
    fun `confirm rejects out-of-range draft`() = runTest {
        val draft = ScanImportDraft(
            value = 99f,
            date = "2026-09-20",
            time = "08:32",
            session = "Sáng",
            dateSource = ScanDateSource.METER,
            timeSource = ScanTimeSource.METER,
            risk = GlucoseRiskEvaluator.evaluate(6f),
            hba1cProvisional = 0.0
        )

        val result = pipeline.confirmAndSave(draft)

        assertTrue(result is ScanSaveResult.Rejected)
        verify(repository, never()).insertLog(any())
    }
}
