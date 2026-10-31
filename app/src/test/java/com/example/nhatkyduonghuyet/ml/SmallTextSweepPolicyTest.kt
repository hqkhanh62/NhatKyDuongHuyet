package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmallTextSweepPolicyTest {

    @Test
    fun `keeps retrying after repeated frames without metadata`() {
        val policy = SmallTextSweepPolicy()
        val fields = MeterDisplayFields(
            glucose = GlucoseReading(
                value = 5.7f,
                confidence = 0.9f,
                fromSpatialLine = true,
                hasUnit = true,
                hasDecimal = true
            )
        )

        repeat(10) { attempt ->
            assertTrue(policy.shouldRun(fields, attempt * 600L))
        }
    }

    @Test
    fun `throttles retries to the minimum interval`() {
        val policy = SmallTextSweepPolicy()
        val fields = MeterDisplayFields()

        assertTrue(policy.shouldRun(fields, 1_000L))
        assertFalse(policy.shouldRun(fields, 1_599L))
        assertTrue(policy.shouldRun(fields, 1_600L))
    }

    @Test
    fun `stops the extra sweep when every display field is recognized`() {
        val policy = SmallTextSweepPolicy()
        val completeFields = MeterDisplayFields(
            glucose = GlucoseReading(
                value = 5.7f,
                confidence = 0.9f,
                fromSpatialLine = true,
                hasUnit = true,
                hasDecimal = true
            ),
            time = MeterTime(hour = 8, minute = 32),
            date = MeterDate(year = 2026, month = 8, day = 20)
        )

        assertFalse(policy.shouldRun(completeFields, 1_000L))
    }
}
