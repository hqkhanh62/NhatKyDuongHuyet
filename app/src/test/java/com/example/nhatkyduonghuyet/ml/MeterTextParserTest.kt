package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm chứng OCR đa tầng trên màn hình máy đo: chỉ số (tầng 1), giờ (tầng 2),
 * ngày (tầng 3) và mã lỗi máy đo. Corpus được giữ đồng bộ với
 * tools/prototype_meter_text_parser.py.
 */
class MeterTextParserTest {

    // ----------------------------------------------------------------- tầng 1
    @Test
    fun `reads a plain mmol reading`() {
        assertEquals(5.7f, MeterTextParser.extractGlucose("5.7 mmol/L"))
    }

    @Test
    fun `prefers the largest line when layout is available`() {
        val lines = listOf(
            OcrLine("DAY", 40),
            OcrLine("5.7", 200),
            OcrLine("mmol/L", 45),
            OcrLine("08:32", 40)
        )
        assertEquals(5.7f, MeterTextParser.extractReadingFromLines(lines)?.value)
    }

    @Test
    fun `big line beats a small avg label`() {
        val lines = listOf(
            OcrLine("AVG 5.9", 40),
            OcrLine("6.4 mmol/L", 210),
            OcrLine("15/09", 38)
        )
        val reading = MeterTextParser.extractReadingFromLines(lines)
        assertEquals(6.4f, reading?.value)
        assertTrue("cần có đơn vị thì độ tin cậy phải cao", (reading?.confidence ?: 0f) >= 0.7f)
    }

    @Test
    fun `returns nothing when only labels are readable`() {
        val lines = listOf(OcrLine("DAY", 40), OcrLine("AVG", 38))
        assertNull(MeterTextParser.extractReadingFromLines(lines))
    }

    // ----------------------------------------------------------------- tầng 2
    @Test
    fun `extracts colon time and reports it as HHmm`() {
        val time = MeterTextParser.extractTime("20/08/2026 08:32\nValue: 6.2 mmol/L")
        assertEquals("08:32", time?.formatted)
    }

    @Test
    fun `prefers the labelled time row`() {
        val time = MeterTextParser.extractTime("Date 20/08/2026\nTime 08:32\n6.2 mmol/L")
        assertEquals("08:32", time?.formatted)
        assertEquals(1.0f, time?.confidence)
    }

    @Test
    fun `handles midnight and late night times`() {
        assertEquals("22:05", MeterTextParser.extractTime("22:05\n5.4 mmol/L")?.formatted)
        assertEquals("00:07", MeterTextParser.extractTime("00:07 5.4 mmol/L")?.formatted)
    }

    @Test
    fun `understands am pm clock formats`() {
        assertEquals("07:30", MeterTextParser.extractTime("7:30 AM 5.9 mmol/L")?.formatted)
        assertEquals("19:45", MeterTextParser.extractTime("7.45 PM 8.8 mmol/L")?.formatted)
    }

    @Test
    fun `ignores seconds suffix but keeps the hour`() {
        assertEquals("09:15", MeterTextParser.extractTime("09:15:30 6.6 mmol/L")?.formatted)
    }

    @Test
    fun `does not invent a time from a bare reading`() {
        assertNull(MeterTextParser.extractTime("6.2 mmol/L"))
        assertNull(MeterTextParser.extractTime("1.2.3"))
        // "5:7" là lỗi OCR của 5.7, không phải giờ.
        assertNull(MeterTextParser.extractTime("5:7 mmol/L"))
    }

    // ----------------------------------------------------------------- tầng 3
    @Test
    fun `reads iso date with label`() {
        val date = MeterTextParser.extractDate("Date 2026-08-20\n6.2 mmol/L", 2026, "2026-09-15")
        assertEquals("2026-08-20", date?.iso)
        assertEquals(0.95f, date?.confidence)
        assertEquals(false, date?.ambiguous)
    }

    @Test
    fun `reads day first date and keeps the time intact`() {
        val date = MeterTextParser.extractDate("20/08/2026 08:32\nValue: 6.2 mmol/L", 2026, "2026-09-15")
        assertEquals("2026-08-20", date?.iso)
    }

    @Test
    fun `detects month first format when day exceeds twelve`() {
        assertEquals("2026-08-22", MeterTextParser.extractDate("08/22/2026 6.2 mmol/L", 2026, "2026-09-15")?.iso)
        assertEquals("2026-08-22", MeterTextParser.extractDate("08-22-2026 6.2 mmol/L", 2026, "2026-09-15")?.iso)
    }

    @Test
    fun `assumes day first when both parts are ambiguous`() {
        val date = MeterTextParser.extractDate("08/09/2026 6.2", 2026, "2026-09-15")
        assertEquals("2026-09-08", date?.iso)
        assertEquals(true, date?.ambiguous)
    }

    @Test
    fun `supports short day month form with fallback year`() {
        val date = MeterTextParser.extractDate("15/09 6.2 mmol/L", 2026, "2026-09-15")
        assertEquals("2026-09-15", date?.iso)
        assertEquals(0.6f, date?.confidence)
    }

    @Test
    fun `repairs swapped month and day in iso form`() {
        val date = MeterTextParser.extractDate("2026-13-05\n6.2 mmol/L", 2026, "2026-09-15")
        assertEquals("2026-05-13", date?.iso)
        assertEquals(true, date?.ambiguous)
    }

    @Test
    fun `supports dotted european date`() {
        assertEquals("2026-08-20", MeterTextParser.extractDate("20.08.2026 6.2 mmol/L", 2026, "2026-09-15")?.iso)
    }

    @Test
    fun `refuses impossible dates and implausible clock drift`() {
        assertNull(MeterTextParser.extractDate("6.2 mmol/L", 2026, "2026-09-15"))
        assertNull(MeterTextParser.extractDate("5.7", 2026, "2026-09-15"))
        // Đồng hồ máy đo chạy nhanh hơn 1 ngày -> không tin, Auto Clean loại.
        assertNull(MeterTextParser.extractDate("2030-01-01 6.2 mmol/L", 2026, "2026-09-15"))
        // 31/04 không tồn tại.
        assertNull(MeterTextParser.extractDate("31/04/2026 6.2 mmol/L", 2026, "2026-09-15"))
    }

    @Test
    fun `does not mistake a dotted time for a date`() {
        assertNull(MeterTextParser.extractDate("08:30 6.2 mmol/L", 2026, "2026-09-15"))
        // Dạng chấm chỉ được tin khi dòng có nhãn Date.
        assertEquals("2026-08-30", MeterTextParser.extractDate("Date 30.08", 2026, "2026-09-15")?.iso)
    }

    // ------------------------------------------------------------- mã lỗi máy đo
    @Test
    fun `reports meter error codes`() {
        assertEquals("E-05", MeterTextParser.detectMeterError("E-05\nInsert strip"))
        assertEquals("HI", MeterTextParser.detectMeterError("HI"))
        assertEquals("LO", MeterTextParser.detectMeterError("LO"))
    }

    @Test
    fun `ignores error looking text when a value is present`() {
        assertNull(MeterTextParser.detectMeterError("6.2 mmol/L"))
        assertNull(MeterTextParser.detectMeterError("Result: 6.2 mmol/L\n08:32"))
    }

    // ------------------------------------------------------------------ cả 3 tầng
    @Test
    fun `parse returns all three layers of a meter screen`() {
        val fields = MeterTextParser.parse("20/08/2026 08:32\nValue: 6.2 mmol/L")
        assertEquals(6.2f, fields.value)
        assertEquals("08:32", fields.time?.formatted)
        assertEquals("2026-08-20", fields.date?.iso)
        assertNull(fields.errorCode)
    }

    @Test
    fun `parse still returns time and date when the value is unreadable`() {
        val fields = MeterTextParser.parse("2026-08-20\nTime 21:15\n--.-")
        assertNull(fields.value)
        assertEquals("21:15", fields.time?.formatted)
        assertEquals("2026-08-20", fields.date?.iso)
    }

    // ------------------------------------------- dong chu nho: mm-dd / hh:mm

    @Test
    fun `repairs digit confusables only next to date and time separators`() {
        assertEquals("14:35", MeterTextParser.repairOcrDigits("l4:3S"))
        assertEquals("09-23", MeterTextParser.repairOcrDigits("O9-23"))
        assertEquals("09-28", MeterTextParser.repairOcrDigits("O9-2B"))
        // Nhan chu va chi so thap phan tuyet doi khong bi viet lai
        assertEquals("Date: Time", MeterTextParser.repairOcrDigits("Date: Time"))
        assertEquals("5.O mmol/L", MeterTextParser.repairOcrDigits("5.O mmol/L"))
    }

    @Test
    fun `splits a glued status row into date and time`() {
        assertEquals("09-23 14:35", MeterTextParser.splitGluedRow("09-2314:35"))
        assertEquals("14:35 09-23", MeterTextParser.splitGluedRow("14:3509-23"))
    }

    @Test
    fun `reads mm-dd and hh-mm from the small status row`() {
        val fields = MeterTextParser.parseSmallText(
            rawText = "09-23 14:35",
            includeGlucose = false,
            fallbackYear = 2026,
            todayIso = "2026-09-23"
        )
        assertEquals("14:35", fields.time?.formatted)
        assertEquals("2026-09-23", fields.date?.iso)
        assertEquals(false, fields.date?.ambiguous)
        assertNull(fields.glucose)
        assertEquals(true, fields.smallTextScanned)
    }

    @Test
    fun `status row digits never become the glucose value`() {
        val fields = MeterTextParser.parseSmallText(
            rawText = "09-23 14:35",
            includeGlucose = true,
            fallbackYear = 2026,
            todayIso = "2026-09-23"
        )
        assertNull(fields.glucose)
    }

    @Test
    fun `sweep recovers the value the tight crop missed`() {
        val fields = MeterTextParser.parseSmallText(
            rawText = "09-23 14:35\n6.2 mmol/L",
            lines = listOf(OcrLine("09-23 14:35", 12), OcrLine("6.2 mmol/L", 90)),
            includeGlucose = true,
            fallbackYear = 2026,
            todayIso = "2026-09-23"
        )
        assertEquals(6.2f, fields.glucose?.value ?: 0f, 0.001f)
    }

    @Test
    fun `dash separated date is month first while slash stays day first`() {
        assertEquals(
            "2026-09-08",
            MeterTextParser.extractDate("09-08-2026 6.2", 2026, "2026-09-15")?.iso
        )
        assertEquals(
            "2026-08-22",
            MeterTextParser.extractDate("08/22/2026 6.2 mmol/L", 2026, "2026-09-15")?.iso
        )
    }

    @Test
    fun `ambiguous orientation falls back when the meter date is impossible`() {
        assertEquals(
            "2026-11-05",
            MeterTextParser.extractDate("11-05 6.2 mmol/L", 2026, "2026-11-05")?.iso
        )
        val drifted = MeterTextParser.extractDate("11-05 6.2 mmol/L", 2026, "2026-09-23")
        assertEquals("2026-05-11", drifted?.iso)
        assertEquals(true, drifted?.ambiguous)
    }

    @Test
    fun `reading closest to the phone date wins a tie`() {
        val date = MeterTextParser.extractDate("09/08 6.2 mmol/L", 2026, "2026-09-08")
        assertEquals("2026-09-08", date?.iso)
        assertEquals(true, date?.ambiguous)
    }

    @Test
    fun `merge keeps the primary source and fills only the gaps`() {
        val base = MeterTextParser.parse("08:30\n6.2 mmol/L")
        val extra = MeterTextParser.parseSmallText(
            rawText = "09-23 14:35",
            fallbackYear = 2026,
            todayIso = "2026-09-23"
        )
        val merged = MeterTextParser.merge(base, extra)
        assertEquals(6.2f, merged.glucose?.value ?: 0f, 0.001f)
        assertEquals("08:30", merged.time?.formatted)
        assertEquals("2026-09-23", merged.date?.iso)
    }
}
