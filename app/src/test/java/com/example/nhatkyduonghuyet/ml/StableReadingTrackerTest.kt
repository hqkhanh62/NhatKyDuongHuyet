package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StableReadingTrackerTest {

    @Test
    fun `delivers after enough identical readings`() {
        val tracker = StableReadingTracker()
        assertNull(tracker.offer(5.7f))
        assertNull(tracker.offer(5.7f))
        assertNull(tracker.offer(5.7f))
        assertEquals(5.7f, tracker.offer(5.7f))
    }

    @Test
    fun `adjacent values are not merged`() {
        // The old ±0.15 tolerance counted 5.6 and 5.7 as the same reading.
        val tracker = StableReadingTracker()
        tracker.offer(5.7f)
        tracker.offer(5.6f)
        tracker.offer(5.7f)
        assertNull(tracker.offer(5.6f))
        assertNull(tracker.offer(5.7f))
        assertEquals(5.7f, tracker.offer(5.7f))
    }

    @Test
    fun `flip flopping values never deliver`() {
        val tracker = StableReadingTracker()
        repeat(10) { i ->
            assertNull(tracker.offer(if (i % 2 == 0) 5.7f else 5.1f))
        }
    }

    @Test
    fun `a repeated wrong value can be outvoted`() {
        // 5.1 was misread twice, then the value settles on 5.7.
        val tracker = StableReadingTracker()
        tracker.offer(5.1f)
        tracker.offer(5.1f)
        tracker.offer(5.7f)
        tracker.offer(5.7f)
        assertNull(tracker.offer(5.7f))
        assertEquals(5.7f, tracker.offer(5.7f))
    }

    @Test
    fun `quantises float noise to one decimal`() {
        val tracker = StableReadingTracker()
        tracker.offer(5.7000003f)
        tracker.offer(5.6999998f)
        tracker.offer(5.7000001f)
        assertEquals(5.7f, tracker.offer(5.7000002f))
    }

    @Test
    fun `clear resets the history`() {
        val tracker = StableReadingTracker()
        repeat(4) { tracker.offer(5.7f) }
        tracker.clear()
        assertNull(tracker.offer(5.7f))
    }
}
