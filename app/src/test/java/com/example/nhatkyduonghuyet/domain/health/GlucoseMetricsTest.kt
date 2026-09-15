package com.example.nhatkyduonghuyet.domain.health

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Định nghĩa số học dùng chung giữa Dashboard và luồng quét camera AI. */
class GlucoseMetricsTest {

    private fun entry(
        date: String,
        session: String = "Sáng",
        before: Double? = null,
        after: Double? = null,
        time: String? = "07:00"
    ) = LogEntry(date = date, session = session, time = time, bgBefore = before, bgAfter = after)

    @Test
    fun `averages each day and keeps chronological order`() {
        val entries = listOf(
            entry("2026-09-15", before = 6.0),
            entry("2026-09-13", before = 5.0, after = 7.0),
            entry("2026-09-14", before = 6.0)
        )
        val averages = GlucoseMetrics.dailyMeasuredAverages(entries)
        assertEquals(listOf("2026-09-13", "2026-09-14", "2026-09-15"), averages.keys.toList())
        assertEquals(6.0f, averages["2026-09-13"]!!)
        assertEquals(6.0f, averages["2026-09-15"]!!)
    }

    @Test
    fun `drops implausible readings`() {
        val entries = listOf(entry("2026-09-13", before = 999.0, after = 1.0))
        assertTrue(GlucoseMetrics.dailyMeasuredAverages(entries).isEmpty())
    }

    @Test
    fun `adds a pending reading to its own day only`() {
        val entries = listOf(
            entry("2026-09-13", before = 5.0),
            entry("2026-09-14", before = 6.0)
        )
        val updated = GlucoseMetrics.dailyAveragesWithReading(entries, "2026-09-14", 7.0f)
        assertEquals(5.0f, updated["2026-09-13"]!!)
        assertEquals(6.5f, updated["2026-09-14"]!!)
        // Chỉ cộng thêm vào ngày đang quét, không tạo ngày mới, không tạo bản ghi mới.
        assertEquals(2, updated.size)
        assertEquals(2, entries.size)
    }

    @Test
    fun `excludes model predictions from the realtime buffer`() {
        val entries = listOf(
            entry("2026-09-13", before = 5.0),
            entry("2026-09-13", session = GlucoseMetrics.SESSION_AI_PREDICTION, before = 20.0),
            entry("2026-09-14", before = 6.0, time = "08:00"),
            entry("2026-09-14", before = 7.0, time = "12:00")
        )
        assertEquals(listOf(5.0f, 6.0f, 7.0f), GlucoseMetrics.chronologicalMeasurements(entries))
    }

    @Test
    fun `weights recent days higher`() {
        assertEquals(2.3333333f, GlucoseMetrics.weightedAverage(listOf(1f, 3f)))
        assertEquals(0f, GlucoseMetrics.weightedAverage(emptyList()))
    }

    @Test
    fun `estimates hba1c from the average`() {
        // (7.4 + 2.59) / 1.59 = 6.283 %
        assertEquals(6.283, GlucoseMetrics.estimateHba1c(7.4f), 0.01)
        assertEquals(0.0, GlucoseMetrics.estimateHba1c(0f), 0.0)
        assertEquals(0.0, GlucoseMetrics.estimateHba1c(Float.NaN), 0.0)
    }

    @Test
    fun `computes time in range`() {
        assertEquals(75, GlucoseMetrics.timeInRangePercent(listOf(3.9f, 5f, 10f, 10.1f)))
        assertEquals(0, GlucoseMetrics.timeInRangePercent(listOf(2f, 20f)))
        assertNull(GlucoseMetrics.timeInRangePercent(emptyList()))
    }

    @Test
    fun `validity window matches the safety policy`() {
        assertTrue(GlucoseMetrics.isValidMmol(2.0))
        assertTrue(GlucoseMetrics.isValidMmol(30.0))
        assertFalse(GlucoseMetrics.isValidMmol(1.9))
        assertFalse(GlucoseMetrics.isValidMmol(30.1))
        assertFalse(GlucoseMetrics.isValidMmol(Double.NaN))
    }
}
