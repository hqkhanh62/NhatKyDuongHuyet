package com.example.nhatkyduonghuyet.ml

/**
 * Pure image operations shared by the seven-segment reader and the OCR pre-processing.
 * No Android types, so every step is unit-testable on the JVM.
 */
object ImageOps {

    /** Classic Otsu threshold over a luminance buffer. */
    fun otsu(luma: IntArray, count: Int = luma.size): Int {
        val histogram = IntArray(256)
        for (i in 0 until count) {
            val value = luma[i]
            histogram[if (value < 0) 0 else if (value > 255) 255 else value]++
        }
        var sumAll = 0.0
        for (value in 0 until 256) sumAll += value.toDouble() * histogram[value]

        var weightBackground = 0.0
        var sumBackground = 0.0
        var best = -1.0
        var threshold = 127
        for (t in 0 until 256) {
            weightBackground += histogram[t]
            if (weightBackground == 0.0) continue
            val weightForeground = count - weightBackground
            if (weightForeground <= 0.0) break
            sumBackground += t.toDouble() * histogram[t]
            val meanBackground = sumBackground / weightBackground
            val meanForeground = (sumAll - sumBackground) / weightForeground
            val between = weightBackground * weightForeground *
                (meanBackground - meanForeground) * (meanBackground - meanForeground)
            if (between > best) {
                best = between
                threshold = t
            }
        }
        return threshold
    }

    /** Ink mask (true = dark) with automatic polarity so inverted displays work too. */
    fun binarize(luma: IntArray, count: Int = luma.size): BooleanArray {
        val threshold = otsu(luma, count)
        val mask = BooleanArray(count) { luma[it] <= threshold }
        var ink = 0
        for (value in mask) if (value) ink++
        if (ink * 2 > count) for (i in mask.indices) mask[i] = !mask[i]
        return mask
    }

    fun inkRatio(mask: BooleanArray): Float {
        if (mask.isEmpty()) return 0f
        var ink = 0
        for (value in mask) if (value) ink++
        return ink.toFloat() / mask.size
    }

    /** Square dilation, separated into a row pass and a column pass. */
    fun dilate(src: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray {
        if (radius < 1) return src
        val horizontal = BooleanArray(src.size)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var on = false
                var dx = -radius
                while (dx <= radius) {
                    val nx = x + dx
                    if (nx in 0 until width && src[row + nx]) {
                        on = true
                        break
                    }
                    dx++
                }
                horizontal[row + x] = on
            }
        }
        val out = BooleanArray(src.size)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var on = false
                var dy = -radius
                while (dy <= radius) {
                    val ny = y + dy
                    if (ny in 0 until height && horizontal[ny * width + x]) {
                        on = true
                        break
                    }
                    dy++
                }
                out[row + x] = on
            }
        }
        return out
    }

    /** Square erosion; pixels outside the canvas count as background. */
    fun erode(src: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray {
        if (radius < 1) return src
        val horizontal = BooleanArray(src.size)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var on = true
                var dx = -radius
                while (dx <= radius) {
                    val nx = x + dx
                    if (nx !in 0 until width || !src[row + nx]) {
                        on = false
                        break
                    }
                    dx++
                }
                horizontal[row + x] = on
            }
        }
        val out = BooleanArray(src.size)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var on = true
                var dy = -radius
                while (dy <= radius) {
                    val ny = y + dy
                    if (ny !in 0 until height || !horizontal[ny * width + x]) {
                        on = false
                        break
                    }
                    dy++
                }
                out[row + x] = on
            }
        }
        return out
    }

    /**
     * Morphological closing. On a seven-segment display this bridges the gaps *between the
     * segments of one glyph*, which is what makes a general purpose OCR engine able to read
     * the digits at all.
     */
    fun close(src: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray =
        if (radius < 1) src else erode(dilate(src, width, height, radius), width, height, radius)

    /** Median horizontal ink run: a scale estimate independent of the zoom level. */
    fun strokeWidth(mask: BooleanArray, width: Int, height: Int): Int {
        val runs = ArrayList<Int>()
        val step = if (height / 60 > 1) height / 60 else 1
        var y = 0
        while (y < height) {
            val row = y * width
            var run = 0
            for (x in 0 until width) {
                if (mask[row + x]) {
                    run++
                } else if (run > 0) {
                    runs += run
                    run = 0
                }
            }
            if (run > 0) runs += run
            y += step
        }
        if (runs.isEmpty()) return 0
        runs.sort()
        return runs[runs.size / 2]
    }
}
