package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Pro AI multi-layer OCR: glucose value + clock (HH:mm) + calendar
 * (DD/MM, MM/DD, YYYY-MM-DD) parsed from one meter screen.
 */
class GlucoseScannerOcrTest {

    private val scanner = GlucoseScanner()

    // ---------- Clock layer ----------

    @Test
    fun `reads HH-mm clock`() {
        assertEquals("08:32", scanner.extractTimeForTesting("6.2 mmol/L 08:32"))
    }

    @Test
    fun `zero-pads single digit hour`() {
        assertEquals("08:05", scanner.extractTimeForTesting("Result 8:05"))
    }

    @Test
    fun `reads dot separated clock from seven segment display`() {
        assertEquals("18:45", scanner.extractTimeForTesting("18.45 7.1"))
    }

    @Test
    fun `reads h separated clock`() {
        assertEquals("08:32", scanner.extractTimeForTesting("8h32"))
    }

    @Test
    fun `rejects impossible clock`() {
        assertNull(scanner.extractTimeForTesting("25:99"))
        assertNull(scanner.extractTimeForTesting("no clock here"))
    }

    // ---------- Calendar layer ----------

    @Test
    fun `reads ISO date`() {
        assertEquals("2026-08-20", scanner.extractDateForTesting("2026-08-20 6.2"))
        assertEquals("2026-08-20", scanner.extractDateForTesting("2026/08/20"))
    }

    @Test
    fun `reads Vietnamese DD-MM-YYYY`() {
        assertEquals("2026-08-20", scanner.extractDateForTesting("20/08/2026 6.2 mmol/L"))
        assertEquals("2026-08-20", scanner.extractDateForTesting("20-08-2026"))
        assertEquals("2026-08-20", scanner.extractDateForTesting("20.08.2026"))
    }

    @Test
    fun `disambiguates MM-DD-YYYY when day exceeds 12`() {
        assertEquals("2026-08-20", scanner.extractDateForTesting("08/20/2026"))
    }

    @Test
    fun `ambiguous pair defaults to Vietnamese DD-MM`() {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        assertEquals("$year-08-11", scanner.extractDateForTesting("11/08"))
    }

    @Test
    fun `short date assumes current year`() {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        assertEquals("$year-08-20", scanner.extractDateForTesting("MEM 20/08 6.2"))
    }

    @Test
    fun `rejects impossible calendar date`() {
        assertNull(scanner.extractDateForTesting("31/02/2026"))
        assertNull(scanner.extractDateForTesting("13/25"))
        assertNull(scanner.extractDateForTesting("no date here"))
    }

    @Test
    fun `short date never matches glucose decimal`() {
        // "5.7" must stay a glucose value, never become a date.
        assertNull(scanner.extractDateForTesting("5.7 mmol/L"))
    }

    // ---------- Full multi-layer parse ----------

    @Test
    fun `parses full meter screen in one pass`() {
        val parse = scanner.parseMeterTextForTesting("Value: 6.2 mmol/L 20/08/2026 08:32")
        assertEquals(6.2f, parse.glucose)
        assertEquals("2026-08-20", parse.date)
        assertEquals("08:32", parse.time)
        assertNotNull(parse.confidence)
    }

    @Test
    fun `parses screen without clock or calendar`() {
        val parse = scanner.parseMeterTextForTesting("6.2 mmol/L")
        assertEquals(6.2f, parse.glucose)
        assertNull(parse.date)
        assertNull(parse.time)
    }
}
