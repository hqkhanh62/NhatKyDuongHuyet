package com.example.nhatkyduonghuyet.scan

import kotlin.math.abs

/**
 * Real-time stability gate for the AI camera loop.
 *
 * A scanned value is only surfaced to the review banner after it repeats
 * [requiredMatches] times inside the sliding [windowSize] window (within
 * [tolerance] mmol/L). This filters flicker and half-rendered frames from
 * the meter display without blocking the live preview.
 */
class ScanStabilityTracker(
    private val windowSize: Int = DEFAULT_WINDOW_SIZE,
    private val requiredMatches: Int = DEFAULT_REQUIRED_MATCHES,
    private val tolerance: Float = DEFAULT_TOLERANCE
) {
    private val recent = ArrayDeque<Float>()

    /**
     * Feeds one frame result. Returns the stable value once the window
     * confirms it, or null while the camera is still settling.
     */
    fun add(value: Float): Float? {
        recent.addLast(value)
        while (recent.size > windowSize) {
            recent.removeFirst()
        }
        return stableValue()
    }

    fun stableValue(): Float? {
        if (recent.size < requiredMatches) return null
        val latest = recent.last()
        val matches = recent.count { abs(it - latest) <= tolerance }
        return if (matches >= requiredMatches) latest else null
    }

    fun reset() {
        recent.clear()
    }

    fun sampleCount(): Int = recent.size

    companion object {
        const val DEFAULT_WINDOW_SIZE = 4
        const val DEFAULT_REQUIRED_MATCHES = 3
        const val DEFAULT_TOLERANCE = 0.15f
    }
}
