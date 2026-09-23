package com.example.nhatkyduonghuyet.domain.scanner

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.ml.GlucoseReading
import com.example.nhatkyduonghuyet.ml.MeterDate
import com.example.nhatkyduonghuyet.ml.MeterDisplayFields
import com.example.nhatkyduonghuyet.ml.MeterTextParser
import com.example.nhatkyduonghuyet.ml.MeterTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Luồng Auto Import: làm sạch dữ liệu, ưu tiên ngày/giờ trên máy đo,
 * tự phân loại buổi, tự chọn ô trước/sau ăn và lên kế hoạch ghi DB.
 */
class AutoImportPipelineTest {

    private fun fieldsWith(text: String): MeterDisplayFields = MeterTextParser.parse(text)

    // ------------------------------------------------------------- Auto Clean
    @Test
    fun `accepts a value inside the safety window`() {
        val result = AutoImportPipeline.clean(5.7f)
        assertTrue(result is AutoImportPipeline.CleanResult.Accepted)
        assertEquals(5.7f, (result as AutoImportPipeline.CleanResult.Accepted).value)
    }

    @Test
    fun `rejects noisy or out of range values`() {
        assertEquals(
            AutoImportPipeline.RejectReason.OUT_OF_RANGE,
            (AutoImportPipeline.clean(1.9f) as AutoImportPipeline.CleanResult.Rejected).reason
        )
        assertEquals(
            AutoImportPipeline.RejectReason.OUT_OF_RANGE,
            (AutoImportPipeline.clean(30.1f) as AutoImportPipeline.CleanResult.Rejected).reason
        )
        assertEquals(
            AutoImportPipeline.RejectReason.NOT_FINITE,
            (AutoImportPipeline.clean(Float.NaN) as AutoImportPipeline.CleanResult.Rejected).reason
        )
        assertEquals(
            AutoImportPipeline.RejectReason.NO_VALUE,
            (AutoImportPipeline.clean(null) as AutoImportPipeline.CleanResult.Rejected).reason
        )
    }

    @Test
    fun `meter error code wins over a half read value`() {
        val result = AutoImportPipeline.clean(null, "E-05")
        assertEquals(AutoImportPipeline.RejectReason.METER_ERROR, (result as AutoImportPipeline.CleanResult.Rejected).reason)
        assertEquals("E-05", result.detail)
    }

    @Test
    fun `quantises to the meter resolution`() {
        assertEquals(3.1f, (AutoImportPipeline.clean(3.14f) as AutoImportPipeline.CleanResult.Accepted).value)
        assertEquals(30.0f, (AutoImportPipeline.clean(30.0f) as AutoImportPipeline.CleanResult.Accepted).value)
    }

    // ------------------------------------------------- ngày/giờ + fallback hệ thống
    @Test
    fun `prefers the meter time and date`() {
        val fields = fieldsWith("Date 2026-08-20\nTime 08:32\n6.2 mmol/L")
        val time = AutoImportPipeline.resolveTime(fields, "23:59")
        val date = AutoImportPipeline.resolveDate(fields, "2026-09-15")
        assertEquals("08:32", time.time)
        assertEquals(FieldSource.METER, time.source)
        assertEquals("2026-08-20", date.date)
        assertEquals(FieldSource.METER, date.source)
    }

    @Test
    fun `falls back to the system clock when the meter shows no time`() {
        val fields = fieldsWith("6.2 mmol/L")
        val time = AutoImportPipeline.resolveTime(fields, "21:15")
        val date = AutoImportPipeline.resolveDate(fields, "2026-09-15")
        assertEquals("21:15", time.time)
        assertEquals(FieldSource.SYSTEM, time.source)
        assertEquals("2026-09-15", date.date)
        assertEquals(FieldSource.SYSTEM, date.source)
    }

    @Test
    fun `draft uses meter hour for session classification`() {
        val fields = MeterDisplayFields(
            glucose = null,
            time = MeterTime(21, 15, 0.8f)
        )
        val draft = AutoImportPipeline.draft(
            value = 6.4f,
            fields = fields,
            systemDate = "2026-09-15",
            systemTime = "07:00",
            existingForDate = emptyList()
        )
        assertEquals("21:15", draft.time)
        assertEquals(GlucoseSession.EVENING, draft.session)
        assertEquals(FieldSource.METER, draft.timeSource)
    }

    // ---------------------------------------------------- tự phân loại buổi
    @Test
    fun `classifies every hour into a session`() {
        assertEquals(GlucoseSession.MORNING, GlucoseSession.fromHour(5))
        assertEquals(GlucoseSession.MORNING, GlucoseSession.fromHour(10))
        assertEquals(GlucoseSession.NOON, GlucoseSession.fromHour(11))
        assertEquals(GlucoseSession.NOON, GlucoseSession.fromHour(13))
        assertEquals(GlucoseSession.AFTERNOON, GlucoseSession.fromHour(14))
        assertEquals(GlucoseSession.AFTERNOON, GlucoseSession.fromHour(17))
        assertEquals(GlucoseSession.EVENING, GlucoseSession.fromHour(18))
        assertEquals(GlucoseSession.EVENING, GlucoseSession.fromHour(23))
        // 00:00 - 04:59 vẫn thuộc buổi Tối của ngày hôm đó.
        assertEquals(GlucoseSession.EVENING, GlucoseSession.fromHour(2))
    }

    @Test
    fun `session labels match the four cards of the day screen`() {
        assertEquals(listOf("Sáng", "Trưa", "Chiều", "Tối"), GlucoseSession.values().map { it.label })
        assertEquals(GlucoseSession.NOON, GlucoseSession.fromLabel("Trưa"))
    }

    // --------------------------------------------------------- ô trước/sau ăn
    @Test
    fun `fills the before meal slot around breakfast`() {
        assertEquals(
            ReadingSlot.BEFORE_MEAL,
            AutoImportPipeline.decideSlot(6, 40, GlucoseSession.MORNING, hasBefore = false, hasAfter = false)
        )
    }

    @Test
    fun `uses the after meal slot two hours after the meal`() {
        assertEquals(
            ReadingSlot.AFTER_MEAL,
            AutoImportPipeline.decideSlot(13, 45, GlucoseSession.NOON, hasBefore = true, hasAfter = false)
        )
        assertEquals(
            ReadingSlot.AFTER_MEAL,
            AutoImportPipeline.decideSlot(14, 30, GlucoseSession.AFTERNOON, hasBefore = true, hasAfter = false)
        )
    }

    @Test
    fun `prefers an empty slot when the time tells nothing`() {
        assertEquals(
            ReadingSlot.BEFORE_MEAL,
            AutoImportPipeline.decideSlot(17, 0, GlucoseSession.AFTERNOON, hasBefore = false, hasAfter = false)
        )
        assertEquals(
            ReadingSlot.AFTER_MEAL,
            AutoImportPipeline.decideSlot(17, 0, GlucoseSession.AFTERNOON, hasBefore = true, hasAfter = false)
        )
        // Cả hai ô đã đầy -> ghi đè ô sau ăn (lần đo muộn nhất trong buổi).
        assertEquals(
            ReadingSlot.AFTER_MEAL,
            AutoImportPipeline.decideSlot(20, 30, GlucoseSession.EVENING, hasBefore = true, hasAfter = true)
        )
    }

    // ---------------------------------------------------------------- plan DB
    @Test
    fun `plans an insert when the session is empty`() {
        val draft = AutoImportPipeline.ScanDraft(
            value = 6.2f,
            date = "2026-09-15",
            time = "08:32",
            session = GlucoseSession.MORNING,
            slot = ReadingSlot.BEFORE_MEAL,
            dateSource = FieldSource.METER,
            timeSource = FieldSource.METER,
            valueSource = "PIXEL",
            confidence = 0.9f,
            ocrText = "08:32 6.2 mmol/L"
        )
        val plan = AutoImportPipeline.plan(draft, emptyList())
        assertTrue(plan is AutoImportPipeline.ImportPlan.Insert)
        val entry = (plan as AutoImportPipeline.ImportPlan.Insert).entry
        assertEquals("2026-09-15", entry.date)
        assertEquals("Sáng", entry.session)
        assertEquals("08:32", entry.time)
        assertEquals(6.2, entry.bgBefore!!, 0.0001)
        assertNull(entry.bgAfter)
        assertTrue(entry.note!!.contains("AI Camera OCR"))
        assertTrue(entry.note!!.contains("giờ máy đo"))
    }

    @Test
    fun `plans an update into the free slot and keeps other fields`() {
        val existing = LogEntry(
            id = 7L,
            date = "2026-09-15",
            session = "Sáng",
            time = "07:00",
            medType = "Metformin",
            dose = "500",
            bgBefore = 5.4
        )
        val draft = AutoImportPipeline.ScanDraft(
            value = 7.8f,
            date = "2026-09-15",
            time = "09:00",
            session = GlucoseSession.MORNING,
            slot = ReadingSlot.AFTER_MEAL,
            dateSource = FieldSource.SYSTEM,
            timeSource = FieldSource.SYSTEM,
            valueSource = "ML_KIT",
            confidence = 0.8f,
            ocrText = "7.8"
        )
        val plan = AutoImportPipeline.plan(draft, listOf(existing))
        assertTrue(plan is AutoImportPipeline.ImportPlan.Update)
        val updated = (plan as AutoImportPipeline.ImportPlan.Update).entry
        assertEquals(7L, updated.id)
        assertEquals(5.4, updated.bgBefore!!, 0.0001)
        assertEquals(7.8, updated.bgAfter!!, 0.0001)
        // Giờ do người dùng nhập trước đó được giữ, không bị đè bởi giờ hệ thống.
        assertEquals("07:00", updated.time)
        assertEquals("Metformin", updated.medType)
        assertEquals(existing, plan.previous)
    }

    @Test
    fun `same value at the same time is not stored twice`() {
        val existing = LogEntry(
            id = 3L,
            date = "2026-09-15",
            session = "Trưa",
            time = "11:30",
            bgBefore = 8.4
        )
        val draft = AutoImportPipeline.ScanDraft(
            value = 8.4f,
            date = "2026-09-15",
            time = "11:30",
            session = GlucoseSession.NOON,
            slot = ReadingSlot.BEFORE_MEAL,
            dateSource = FieldSource.SYSTEM,
            timeSource = FieldSource.SYSTEM,
            valueSource = "PIXEL",
            confidence = 0.9f,
            ocrText = "8.4"
        )
        assertTrue(AutoImportPipeline.plan(draft, listOf(existing)) is AutoImportPipeline.ImportPlan.Duplicate)
    }

    @Test
    fun `scanning another session of the same day inserts a new row`() {
        val morning = LogEntry(id = 1L, date = "2026-09-15", session = "Sáng", time = "06:30", bgBefore = 5.1)
        val draft = AutoImportPipeline.ScanDraft(
            value = 9.1f,
            date = "2026-09-15",
            time = "21:00",
            session = GlucoseSession.EVENING,
            slot = ReadingSlot.BEFORE_MEAL,
            dateSource = FieldSource.SYSTEM,
            timeSource = FieldSource.SYSTEM,
            valueSource = "PIXEL",
            confidence = 0.95f,
            ocrText = "9.1"
        )
        val plan = AutoImportPipeline.plan(draft, listOf(morning))
        assertTrue(plan is AutoImportPipeline.ImportPlan.Insert)
    }

    // ------------------------------------------------------------------ utils
    @Test
    fun `stores one decimal without float noise`() {
        assertEquals(5.7, AutoImportPipeline.toStoredMmol(5.7f), 0.0)
        assertEquals(10.1, AutoImportPipeline.toStoredMmol(10.1f), 0.0)
    }

    @Test
    fun `formats and normalises user input`() {
        assertEquals("6.2", AutoImportPipeline.formatMmol(6.2f))
        assertEquals("08:05", AutoImportPipeline.normalizeTime(" 8:5 "))
        assertEquals("21:15", AutoImportPipeline.normalizeTime("21:15"))
        assertNull(AutoImportPipeline.normalizeTime("25:00"))
        assertNull(AutoImportPipeline.normalizeTime("không có giờ"))
        assertEquals("2026-09-15", AutoImportPipeline.normalizeDate("15/09/2026"))
        assertEquals("2026-09-15", AutoImportPipeline.normalizeDate("2026-09-15"))
        assertEquals("2026-09-15", AutoImportPipeline.normalizeDate("15-09-2026"))
        assertNull(AutoImportPipeline.normalizeDate("15/13/2026"))
        assertNotNull(AutoImportPipeline.normalizeDate("15/09"))
    }

    @Test
    fun `draft keeps a meter date that the parser accepted`() {
        val fields = MeterDisplayFields(
            date = MeterDate(2026, 8, 20, 0.9f, ambiguous = false)
        )
        val draft = AutoImportPipeline.draft(
            value = 6.0f,
            fields = fields,
            systemDate = "2026-09-15",
            systemTime = "08:00",
            existingForDate = emptyList()
        )
        assertEquals("2026-08-20", draft.date)
        assertEquals("20/08/2026", draft.dateText)
        assertEquals(FieldSource.METER, draft.dateSource)
    }
    @Test
    fun `draft carries the swapped date warning to the review banner`() {
        val fields = MeterDisplayFields(
            glucose = GlucoseReading(6.2f, 0.9f, fromSpatialLine = true, hasUnit = true, hasDecimal = true),
            time = MeterTime(14, 35),
            date = MeterDate(year = 2026, month = 9, day = 8, confidence = 0.6f, ambiguous = true)
        )
        val draft = AutoImportPipeline.draft(
            value = 6.2f,
            fields = fields,
            systemDate = "2026-09-23",
            systemTime = "20:00",
            existingForDate = emptyList()
        )
        assertTrue(draft.dateAmbiguous)
        assertEquals("2026-09-08", draft.date)

        val certain = fields.copy(date = fields.date?.copy(ambiguous = false))
        assertEquals(
            false,
            AutoImportPipeline.draft(6.2f, certain, "2026-09-23", "20:00", emptyList()).dateAmbiguous
        )
    }
}
