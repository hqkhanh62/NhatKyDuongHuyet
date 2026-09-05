package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlucoseTextParserTest {

    private fun value(text: String): Float? = GlucoseTextParser.parse(text)?.value

    // ---------------------------------------------------------------- happy paths

    @Test
    fun `reads a plain mmol reading`() {
        assertEquals(5.7f, value("5.7 mmol/L"))
    }

    @Test
    fun `reads a comma decimal`() {
        assertEquals(6.1f, value("Glucose: 6,1 mmol/L"))
    }

    @Test
    fun `reads spaced decimal separators`() {
        assertEquals(6.1f, value("Result 6 . 1 mmol/L"))
        assertEquals(6.1f, value("Result 6 1 mmol/L"))
    }

    @Test
    fun `reads a value without a unit when it has a decimal point`() {
        assertEquals(5.7f, value("5.7"))
    }

    @Test
    fun `converts an explicit mg per dl reading`() {
        val parsed = GlucoseTextParser.parse("Result: 126 mg/dL")
        assertNotNull(parsed)
        assertEquals(7.0f, parsed!!.value, 0.01f)
        assertTrue(parsed.convertedFromMgDl)
    }

    @Test
    fun `fixes letters that OCR confuses with digits inside a number`() {
        assertEquals(6.1f, value("Glucose: 6.l mmol/L"))
        assertEquals(12.3f, value("l2.3 mmol/L"))
    }

    // ------------------------------------------------------- safety critical rules

    @Test
    fun `LO is a hypoglycaemia warning and never the number ten`() {
        assertNull(value("Lo"))
        assertNull(value("LO mmol/L"))
        assertEquals(MeterStatus.LOW, GlucoseTextParser.status("Lo"))
    }

    @Test
    fun `HI is reported as a status`() {
        assertNull(value("HI"))
        assertEquals(MeterStatus.HIGH, GlucoseTextParser.status("HI mmol/L"))
    }

    @Test
    fun `error codes are not readings`() {
        assertNull(value("E-3"))
        assertEquals(MeterStatus.ERROR, GlucoseTextParser.status("E-3"))
        assertNull(value("Err"))
    }

    @Test
    fun `a clock that lost its colon is not a reading`() {
        assertNull(value("10 24"))
        assertNull(value("08 32"))
        assertNull(value("1024"))
    }

    @Test
    fun `average and memory screens are rejected`() {
        assertNull(value("AVG 7.2"))
        assertNull(value("14 day avg 7.2"))
        assertNull(value("HbA1c 6.5 %"))
        assertNull(value("Ketone 0.6"))
    }

    @Test
    fun `a bare integer is never guessed as mmol`() {
        assertNull(value("57"))
        assertNull(value("8"))
        assertNull(value("126"))
    }

    @Test
    fun `implausible values are rejected`() {
        assertNull(value("0.8 mmol/L"))
        assertNull(value("48.5 mmol/L"))
    }

    // ------------------------------------------------------------- line handling

    @Test
    fun `keeps the value when a time shares the line`() {
        assertEquals(6.1f, value("6.1 12:45"))
        assertEquals(6.2f, value("20-08 6.2"))
        assertEquals(5.7f, value("08:32   5.7 mmol/L"))
    }

    @Test
    fun `ignores a date row and takes the reading row`() {
        assertEquals(6.2f, value("20/08/2026 08:32\nValue: 6.2 mmol/L"))
    }

    @Test
    fun `prefers the line carrying the unit`() {
        val parsed = GlucoseTextParser.parse("7.7\n5.7 mmol/L")
        assertEquals(5.7f, parsed?.value)
    }

    @Test
    fun `unit text alone is not a number`() {
        assertNull(value("mmol/L"))
        assertNull(value("mg/dL"))
    }

    @Test
    fun `mg per dl integer is accepted when the caller allows it`() {
        val parsed = GlucoseTextParser.parseLine("126", allowBareInteger = true)
        assertNotNull(parsed)
        assertEquals(7.0f, parsed!!.value, 0.01f)
    }

    @Test
    fun `evidence flags describe the match`() {
        val parsed = GlucoseTextParser.parse("Glucose 5.7 mmol/L")!!
        assertTrue(parsed.hasUnit)
        assertTrue(parsed.hasDecimal)
        assertTrue(parsed.hasLabel)
        assertTrue(parsed.isStrong)
    }
}
