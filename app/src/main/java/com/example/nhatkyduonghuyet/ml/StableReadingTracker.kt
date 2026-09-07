package com.example.nhatkyduonghuyet.ml

/**
 * Majority-vote stabiliser for streamed scanner readings.
 *
 * The old filter (3 of the last 4 readings within ±0.15 of the *latest*) had
 * two flaws: values on a meter are quantised to 0.1, so "0.15 tolerance"
 * counted 5.6 and 5.7 as the same reading, and it returned the newest value
 * instead of the most frequent one. A flip-flopping OCR (5.7 / 5.1) could
 * therefore deliver the wrong number as soon as the wrong side repeated.
 *
 * This tracker quantises each reading to one decimal (the meter's own
 * resolution) and accepts the modal value only when it dominates the window.
 */
class StableReadingTracker(
    private val windowSize: Int = DEFAULT_WINDOW_SIZE,
    private val requiredMatches: Int = DEFAULT_REQUIRED_MATCHES
) {

    private val window = ArrayDeque<Float>()

    /**
     * Adds a reading and returns the stable value once enough identical
     * (0.1-quantised) readings have accumulated, or null otherwise.
     */
    @Synchronized
    fun offer(value: Float): Float? {
        if (!value.isFinite()) return null
        window.addLast(kotlin.math.round(value * 10f) / 10f)
        while (window.size > windowSize) window.removeFirst()
        if (window.size < requiredMatches) return null

        var bestValue = 0f
        var bestCount = 0
        window.groupingBy { it }.eachCount().forEach { (candidate, count) ->
            if (count > bestCount) {
                bestCount = count
                bestValue = candidate
            }
        }
        return if (bestCount >= requiredMatches) bestValue else null
    }

    @Synchronized
    fun clear() {
        window.clear()
    }

    private companion object {
        /** Number of consecutive-analysed frames kept (4 fps -> ~1.5 s of history). */
        const val DEFAULT_WINDOW_SIZE = 6

        /** Identical readings required inside the window before delivery. */
        const val DEFAULT_REQUIRED_MATCHES = 4
    }
}
