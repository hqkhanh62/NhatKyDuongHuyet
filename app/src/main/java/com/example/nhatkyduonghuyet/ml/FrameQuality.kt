package com.example.nhatkyduonghuyet.ml

import kotlin.math.abs

/**
 * Cheap frame quality metrics used to drop camera frames that cannot produce a trustworthy
 * reading. Pure JVM code so the thresholds are covered by unit tests.
 *
 * Rejecting a bad frame costs nothing (the next frame arrives in a few tens of milliseconds)
 * while reading a blurred or glared display is how wrong values are produced.
 */
object FrameQuality {

    /** Result of inspecting one region of interest. */
    data class Report(
        /** Laplacian variance normalised by contrast, so it does not depend on exposure. */
        val focus: Float,
        val clippedRatio: Float,
        val contrast: Int
    ) {
        val isBlurred: Boolean get() = focus < MIN_FOCUS
        val hasGlare: Boolean get() = clippedRatio > MAX_CLIPPED_RATIO
        val isFlat: Boolean get() = contrast < MIN_CONTRAST
        val isUsable: Boolean get() = !isBlurred && !hasGlare && !isFlat
    }

    /** Variance of the Laplacian: the standard focus measure. */
    fun sharpness(luma: IntArray, width: Int, height: Int): Float {
        if (width < 3 || height < 3) return 0f
        var sum = 0.0
        var sumSquares = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val index = row + x
                val laplacian = (4 * luma[index] -
                    luma[index - 1] - luma[index + 1] -
                    luma[index - width] - luma[index + width]).toDouble()
                sum += laplacian
                sumSquares += laplacian * laplacian
                count++
            }
        }
        if (count == 0) return 0f
        val mean = sum / count
        return ((sumSquares / count) - mean * mean).toFloat()
    }

    /** Fraction of pixels that are clipped to white — the signature of a specular reflection. */
    fun clippedRatio(luma: IntArray, width: Int, height: Int): Float {
        val count = width * height
        if (count <= 0) return 0f
        var clipped = 0
        for (i in 0 until count) if (luma[i] >= CLIPPED_LEVEL) clipped++
        return clipped.toFloat() / count
    }

    /** Robust contrast: distance between the 5th and 95th luminance percentiles. */
    fun contrast(luma: IntArray, width: Int, height: Int): Int {
        val count = width * height
        if (count <= 0) return 0
        val histogram = IntArray(256)
        for (i in 0 until count) {
            val value = luma[i]
            histogram[if (value < 0) 0 else if (value > 255) 255 else value]++
        }
        val low = percentile(histogram, count, 0.05f)
        val high = percentile(histogram, count, 0.95f)
        return abs(high - low)
    }

    fun inspect(luma: IntArray, width: Int, height: Int): Report {
        val spread = contrast(luma, width, height)
        val variance = sharpness(luma, width, height)
        // Normalising by contrast makes the focus score comparable between a bright LCD and
        // a dim one; a raw Laplacian variance would just measure exposure.
        val focus = if (spread <= 0) 0f else variance / (spread.toFloat() * spread)
        return Report(focus = focus, clippedRatio = clippedRatio(luma, width, height), contrast = spread)
    }

    private fun percentile(histogram: IntArray, total: Int, fraction: Float): Int {
        val target = (total * fraction).toInt()
        var accumulated = 0
        for (value in 0 until 256) {
            accumulated += histogram[value]
            if (accumulated >= target) return value
        }
        return 255
    }

    /** Below this normalised focus score the crop is too soft for a reliable reading. */
    const val MIN_FOCUS = 0.0003f

    /** Above this fraction of blown-out pixels the display is washed out by a reflection. */
    const val MAX_CLIPPED_RATIO = 0.06f

    /** Below this percentile spread there is no readable display in the crop. */
    const val MIN_CONTRAST = 28

    private const val CLIPPED_LEVEL = 253
}
