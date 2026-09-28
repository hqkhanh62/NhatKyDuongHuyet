package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlucoseScannerTest {

    private val scanner = GlucoseScanner()

    @Test
    fun `accepts comma decimal and mmol unit`() {
        assertEquals(6.1f, scanner.extractGlucoseForTesting("Glucose: 6,1 mmol/L"))
    }

    @Test
    fun `accepts spaces around decimal separator`() {
        assertEquals(6.1f, scanner.extractGlucoseForTesting("Result 6 . 1 mmol/L"))
    }

    @Test
    fun `accepts split seven segment decimal digits`() {
        assertEquals(5.7f, scanner.extractGlucoseForTesting("5 7 mmol/L"))
        assertEquals(10.1f, scanner.extractGlucoseForTesting("10 . 1 mmol/L"))
    }

    @Test
    fun `accepts seven segment space decimal`() {
        assertEquals(6.1f, scanner.extractGlucoseForTesting("Result 6 1 mmol/L"))
    }

    @Test
    fun `converts explicit mg per dl`() {
        assertEquals(110f / 18f, scanner.extractGlucoseForTesting("Result: 110 mg/dL"))
    }

    @Test
    fun `ignores numbers belonging to date and selects glucose`() {
        assertEquals(
            6.2f,
            scanner.extractGlucoseForTesting("20/08/2026 08:32\nValue: 6.2 mmol/L")
        )
    }

    @Test
    fun `does not guess unit for large number`() {
        assertNull(scanner.extractGlucoseForTesting("Result: 81"))
    }

    @Test
    fun `accepts 5 point 7 as a normal mmol value`() {
        assertEquals(5.7f, scanner.extractGlucoseForTesting("5.7 mmol/L"))
    }

    @Test
    fun `rejects suspicious 28 point 0 without explicit mmol unit`() {
        assertNull(scanner.extractGlucoseForTesting("28.0"))
    }

    @Test
    fun `normalizes common character substitutions`() {
        assertEquals(6.1f, scanner.extractGlucoseForTesting("Glucose: 6.l mmol/L"))
    }

    @Test
    fun `rejects implausible mmol value`() {
        assertNull(scanner.extractGlucoseForTesting("Glucose: 0.8 mmol/L"))
    }

    // ------------------------------------------------------------------
    // Dropped decimal separator (classic seven-segment OCR failure)
    // ------------------------------------------------------------------

    @Test
    fun `recovers dropped decimal point with mmol unit`() {
        assertEquals(5.7f, scanner.extractGlucoseForTesting("57 mmol/L"))
        assertEquals(10.1f, scanner.extractGlucoseForTesting("101 mmol/L"))
        assertEquals(4.5f, scanner.extractGlucoseForTesting("Glucose: 45 mmol/L"))
    }

    @Test
    fun `does not recover dropped decimals without a unit`() {
        assertNull(scanner.extractGlucoseForTesting("57"))
        assertNull(scanner.extractGlucoseForTesting("Result: 81"))
    }

    // ------------------------------------------------------------------
    // Letter confusables must not turn labels into numbers
    // ------------------------------------------------------------------

    @Test
    fun `letter only tokens need a unit context`() {
        // A meter's low indicator must never be read as 10.0.
        assertNull(scanner.extractGlucoseForTesting("Lo"))
        assertNull(scanner.extractGlucoseForTesting("Result: Lo"))
    }

    @Test
    fun `mixed letter digit tokens are still accepted`() {
        assertEquals(10.0f, scanner.extractGlucoseForTesting("Result: I0"))
    }

    // ------------------------------------------------------------------
    // Colon read instead of a decimal point
    // ------------------------------------------------------------------

    @Test
    fun `converts colon between single digits to a decimal point`() {
        assertEquals(5.7f, scanner.extractGlucoseForTesting("5:7 mmol/L"))
    }

    @Test
    fun `keeps real times untouched`() {
        assertEquals(
            6.2f,
            scanner.extractGlucoseForTesting("20/08/2026 08:32\nValue: 6.2 mmol/L")
        )
    }

    // ------------------------------------------------------------------
    // Hybrid combination policy
    // ------------------------------------------------------------------

    @Test
    fun `high confidence pixel reading beats a disagreeing ml kit value`() {
        val result = scanner.combineHybridForTesting(
            PixelDisplayReading("5.7", 5.7f, 0.95f), 5.1f
        )
        assertEquals(5.7f, result?.value)
        assertEquals("PIXEL", result?.source)
    }

    @Test
    fun `low confidence pixel disagreement refuses the frame`() {
        assertNull(
            scanner.combineHybridForTesting(PixelDisplayReading("5.7", 5.7f, 0.80f), 5.1f)
        )
    }

    @Test
    fun `pixel reading wins when ml kit found nothing`() {
        val result = scanner.combineHybridForTesting(
            PixelDisplayReading("6.4", 6.4f, 0.86f), null
        )
        assertEquals(6.4f, result?.value)
        assertEquals("PIXEL", result?.source)
    }

    @Test
    fun `agreement prefers the pixel value`() {
        val result = scanner.combineHybridForTesting(
            PixelDisplayReading("5.7", 5.7f, 0.86f), 5.8f
        )
        assertEquals(5.7f, result?.value)
        assertEquals("PIXEL", result?.source)
    }

    @Test
    fun `falls back to ml kit when the pixel reader has nothing`() {
        val result = scanner.combineHybridForTesting(null, 6.1f)
        assertEquals(6.1f, result?.value)
        assertEquals("ML_KIT", result?.source)
    }
}
