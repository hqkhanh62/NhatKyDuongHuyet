package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StabilityVoterTest {

    @Test
    fun `needs three identical frames`() {
        val voter = StabilityVoter()
        assertNull(voter.offer(5.7f))
        assertNull(voter.offer(5.7f))
        assertEquals(5.7f, voter.offer(5.7f))
    }

    @Test
    fun `nearby values do not count as the same reading`() {
        val voter = StabilityVoter()
        assertNull(voter.offer(5.7f))
        assertNull(voter.offer(5.8f))
        assertNull(voter.offer(5.7f))
        assertNull(voter.offer(5.8f))
    }

    @Test
    fun `too many contradictions block the delivery`() {
        val voter = StabilityVoter()
        voter.offer(5.7f)
        voter.offer(9.1f)
        voter.offer(5.7f)
        voter.offer(12.3f)
        assertNull(voter.offer(5.7f))
    }

    @Test
    fun `a single odd frame is tolerated`() {
        val voter = StabilityVoter()
        voter.offer(5.7f)
        voter.offer(9.1f)
        voter.offer(5.7f)
        assertEquals(5.7f, voter.offer(5.7f))
    }

    @Test
    fun `clearing forgets the window`() {
        val voter = StabilityVoter()
        voter.offer(5.7f)
        voter.offer(5.7f)
        voter.clear()
        assertEquals(0, voter.size)
        assertNull(voter.offer(5.7f))
    }

    @Test
    fun `stale values leave the window`() {
        val voter = StabilityVoter(required = 3, windowSize = 4)
        assertNull(voter.offer(9.9f))
        assertNull(voter.offer(9.9f))
        assertNull(voter.offer(5.7f))
        // Window: 9.9, 9.9, 5.7, 5.7 — still two contradictions.
        assertNull(voter.offer(5.7f))
        // Window: 9.9, 5.7, 5.7, 5.7 — the oldest 9.9 dropped out, one contradiction left.
        assertEquals(5.7f, voter.offer(5.7f))
    }
}
