package com.example.nhatkyduonghuyet.ml

/**
 * Confirms a reading across several camera frames.
 *
 * Frames of a static scene share the same systematic error, so "most frequent value within a
 * tolerance" would happily confirm a wrong number. This voter therefore requires an exact
 * repeat and refuses to deliver anything while the window still holds contradicting values.
 */
class StabilityVoter(
    private val required: Int = STABILITY_REQUIRED_MATCHES,
    private val windowSize: Int = STABILITY_WINDOW_SIZE
) {
    private val values = ArrayDeque<Float>()

    /** Adds a frame result and returns the confirmed value, or null if not stable yet. */
    fun offer(value: Float): Float? {
        values.addLast(value)
        while (values.size > windowSize) values.removeFirst()
        if (values.size < required) return null

        val counts = values.groupingBy { it }.eachCount()
        val best = counts.maxByOrNull { it.value } ?: return null
        if (best.value < required) return null
        val contradictions = counts.filterKeys { it != best.key }.values.sum()
        if (contradictions > MAX_CONTRADICTIONS) return null
        return best.key
    }

    /** Drops the window, e.g. after a frame that could not be read at all. */
    fun clear() = values.clear()

    val size: Int get() = values.size

    private companion object {
        /** One odd frame is tolerated; two mean the scene is not being read reliably. */
        const val MAX_CONTRADICTIONS = 1
    }
}
