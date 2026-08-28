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
}
