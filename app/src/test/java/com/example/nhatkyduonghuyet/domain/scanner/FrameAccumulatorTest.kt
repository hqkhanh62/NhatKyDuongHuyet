package com.example.nhatkyduonghuyet.domain.scanner

import com.example.nhatkyduonghuyet.ml.StableReadingTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bộ gom frame: Auto Clean trước, bỏ phiếu ổn định sau. */
class FrameAccumulatorTest {

    private fun accumulator() = FrameAccumulator(StableReadingTracker(windowSize = 6, requiredMatches = 4))

    @Test
    fun `locks only after enough identical readings`() {
        val accumulator = accumulator()
        repeat(3) {
            val outcome = accumulator.accept(5.7f)
            assertTrue("frame $it chưa được khoá", outcome is FrameOutcome.Progress)
        }
        val locked = accumulator.accept(5.7f)
        assertTrue(locked is FrameOutcome.Locked)
        assertEquals(5.7f, (locked as FrameOutcome.Locked).stableValue)
    }

    @Test
    fun `flip flopping readings never lock`() {
        val accumulator = accumulator()
        listOf(5.7f, 5.1f, 5.7f, 5.1f, 5.7f, 5.1f).forEach { value ->
            assertTrue(accumulator.accept(value) is FrameOutcome.Progress)
        }
    }

    @Test
    fun `an out of range frame clears the votes`() {
        val accumulator = accumulator()
        repeat(3) { accumulator.accept(5.7f) }
        val noise = accumulator.accept(1.4f)
        assertTrue(noise is FrameOutcome.Nothing)
        assertEquals(AutoImportPipeline.RejectReason.OUT_OF_RANGE, (noise as FrameOutcome.Nothing).reason)
        // Sau khi bị nhiễu chen ngang, một lần đọc lại phải bỏ phiếu từ đầu.
        assertTrue(accumulator.accept(5.7f) is FrameOutcome.Progress)
    }

    @Test
    fun `meter error is reported instead of a value`() {
        val accumulator = accumulator()
        val outcome = accumulator.accept(null, "E-05")
        assertTrue(outcome is FrameOutcome.Nothing)
        assertEquals(AutoImportPipeline.RejectReason.METER_ERROR, (outcome as FrameOutcome.Nothing).reason)
        assertEquals("E-05", outcome.detail)
    }

    @Test
    fun `reset clears progress and live value`() {
        val accumulator = accumulator()
        accumulator.accept(5.7f)
        assertEquals(5.7f, accumulator.lastLiveValue())
        accumulator.reset()
        assertNull(accumulator.lastLiveValue())
    }
}
