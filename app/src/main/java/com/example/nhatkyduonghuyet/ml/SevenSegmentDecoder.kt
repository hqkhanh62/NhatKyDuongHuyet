package com.example.nhatkyduonghuyet.ml

/**
 * Reads a seven-segment glucose value directly from display pixels.
 *
 * Replaces the old fixed-cell reader: digits are located with classic computer
 * vision (Otsu binarization -> morphological closing -> connected components ->
 * column grouping) instead of sampling three hardcoded rectangles. This keeps
 * working when the meter is held closer/farther, when label text or icons share
 * the crop, when the display is backlit (dark background) or when the frame
 * has glare or sensor noise - the cases that made the old reader misread.
 *
 * Everything operates on an ARGB [IntArray] so the decoder is unit-testable on
 * the JVM; [PixelGlucoseReader] is the thin Bitmap adapter.
 */
object SevenSegmentDecoder {

    /** A decoded display reading. */
    data class Decoding(
        val raw: String,
        val value: Float,
        val confidence: Float
    )

    /** Smallest crop we are willing to analyse. */
    private const val MIN_DIM = 16

    /** Otsu thresholds outside this range mean a flat frame (blur/glare): refuse. */
    private const val OTSU_MIN = 8
    private const val OTSU_MAX = 247

    /** Mean luminance below this means a dark (backlit) display: invert polarity. */
    private const val DARK_BG_MEAN = 110f

    /** Luminance band above the threshold counted as "weakly lit" segments. */
    private const val GRAY_ZONE = 15f

    /** Dark-pixel ratio inside a sampling rectangle that marks a segment as lit. */
    private const val SEGMENT_ON_THRESHOLD = 0.30f

    /** Ambiguous-segment penalty (weak 8-vs-0 / 5-vs-6 splits). */
    private const val GRAY_PENALTY = 0.8f

    /** Minimum per-digit confidence, otherwise the whole frame is refused. */
    private const val DIGIT_CONFIDENCE = 0.70f

    /** Minimum confidence for the assembled reading. */
    private const val READING_CONFIDENCE = 0.78f

    /** Applied when no decimal blob was found (it was probably missed). */
    private const val NO_DECIMAL_PENALTY = 0.90f

    /** A component this many times taller than wide is the digit "1". */
    private const val NARROW_ONE_ASPECT = 2.9f

    private const val MIN_COMPONENT_AREA_FRAC = 0.00002f
    private const val MIN_COMPONENT_H_FRAC = 0.045f
    private const val MIN_COMPONENT_AREA_PX = 8
    private const val MAX_CLOSING_RADIUS = 5

    /**
     * Sampling rectangles sit strictly INSIDE the canonical seven-segment
     * geometry so adjacent segments cannot bleed into each other's sample
     * (the classic 7 -> 3 and 4 -> 1 confusions).
     */
    private val SAMPLE_RECTS: Map<Segment, FloatArray> = mapOf(
        Segment.A to floatArrayOf(0.20f, 0.040f, 0.80f, 0.090f),
        Segment.B to floatArrayOf(0.860f, 0.170f, 0.970f, 0.400f),
        Segment.C to floatArrayOf(0.860f, 0.600f, 0.970f, 0.830f),
        Segment.D to floatArrayOf(0.20f, 0.910f, 0.80f, 0.960f),
        Segment.E to floatArrayOf(0.030f, 0.600f, 0.140f, 0.830f),
        Segment.F to floatArrayOf(0.030f, 0.170f, 0.140f, 0.400f),
        Segment.G to floatArrayOf(0.20f, 0.470f, 0.80f, 0.530f)
    )

    private val DIGIT_PATTERNS: List<Set<Segment>> = listOf(
        setOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.E, Segment.F), // 0
        setOf(Segment.B, Segment.C),                                             // 1
        setOf(Segment.A, Segment.B, Segment.G, Segment.E, Segment.D),            // 2
        setOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.G),            // 3
        setOf(Segment.F, Segment.G, Segment.B, Segment.C),                       // 4
        setOf(Segment.A, Segment.F, Segment.G, Segment.C, Segment.D),            // 5
        setOf(Segment.A, Segment.F, Segment.G, Segment.E, Segment.C, Segment.D), // 6
        setOf(Segment.A, Segment.B, Segment.C),                                  // 7
        setOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.E, Segment.F, Segment.G), // 8
        setOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.F, Segment.G)  // 9
    )

    private enum class Segment { A, B, C, D, E, F, G }

    private class Component {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = -1
        var maxY = -1
        var area = 0

        val width: Int get() = maxX - minX + 1
        val height: Int get() = maxY - minY + 1
        val centerX: Float get() = (minX + maxX) / 2f
        val centerY: Float get() = (minY + maxY) / 2f
    }

    private class Glyph(first: Component) {
        var minX = first.minX
        var minY = first.minY
        var maxX = first.maxX
        var maxY = first.maxY
        val parts = mutableListOf(first)

        val width: Int get() = maxX - minX + 1
        val height: Int get() = maxY - minY + 1
        val centerX: Float get() = (minX + maxX) / 2f

        fun add(c: Component) {
            minX = minOf(minX, c.minX)
            minY = minOf(minY, c.minY)
            maxX = maxOf(maxX, c.maxX)
            maxY = maxOf(maxY, c.maxY)
            parts.add(c)
        }
    }

    /**
     * @param pixels ARGB pixels of the cropped display area (Bitmap.getPixels layout).
     * @return the decoded reading, or null when the frame should be refused.
     */
    fun decode(pixels: IntArray, width: Int, height: Int): Decoding? {
        if (width < MIN_DIM || height < MIN_DIM || pixels.size < width * height) return null
        val total = width * height

        // 1. Luminance + Otsu threshold.
        val lums = IntArray(total)
        val hist = IntArray(256)
        for (i in 0 until total) {
            val p = pixels[i]
            val v = (((p shr 16) and 0xFF) * 299 +
                ((p shr 8) and 0xFF) * 587 +
                (p and 0xFF) * 114) / 1000
            lums[i] = v
            hist[v.coerceIn(0, 255)]++
        }
        val threshold = otsu(hist, total)
        if (threshold < OTSU_MIN || threshold > OTSU_MAX) return null

        var sum = 0f
        for (v in lums) sum += v
        val darkBackground = sum / total < DARK_BG_MEAN

        // 2. Binarize. "gray" marks weakly lit pixels just past the threshold:
        // a segment that is half-gray makes 8-vs-0 ambiguous and the digit is
        // rejected instead of guessed.
        val binary = IntArray(total)
        val gray = IntArray(total)
        for (i in 0 until total) {
            val v = lums[i]
            if (darkBackground) {
                binary[i] = if (v > threshold) 1 else 0
                gray[i] = if (v >= threshold - GRAY_ZONE && v <= threshold) 1 else 0
            } else {
                binary[i] = if (v <= threshold) 1 else 0
                gray[i] = if (v > threshold && v <= threshold + GRAY_ZONE) 1 else 0
            }
        }

        // 3. Morphological closing bridges the hairline gaps between the
        //    segments of one digit without merging neighbouring digits.
        val radius = (minOf(width, height) / 70).coerceIn(1, MAX_CLOSING_RADIUS)
        val closed = closing(binary, width, height, radius)

        // 4. Connected components.
        val components = connectedComponents(closed, width, height)
        if (components.isEmpty()) return null

        val minArea = maxOf(MIN_COMPONENT_AREA_PX, (MIN_COMPONENT_AREA_FRAC * total).toInt())
        val minHeight = maxOf(10, (MIN_COMPONENT_H_FRAC * height).toInt())
        val candidates = components.filter {
            it.area >= minArea && it.height >= minHeight && it.width >= 2
        }
        if (candidates.isEmpty()) return null

        // 5. Group parts into glyphs by horizontal overlap only. A distance
        //    rule is unsafe: a narrow "1" leaves only the inter-cell gap
        //    between it and the next digit. Parts of one glyph must also be
        //    vertically close, so label text lit together with the digits
        //    (backlit meters) cannot be merged into a glyph.
        val sorted = candidates.sortedBy { it.minX }
        val glyphs = mutableListOf<Glyph>()
        for (c in sorted) {
            var placed = false
            for (g in glyphs) {
                val overlap = minOf(g.maxX, c.maxX) - maxOf(g.minX, c.minX) + 1
                val minWidth = minOf(g.width, c.width)
                val vGap = maxOf(g.minY - c.maxY, c.minY - g.maxY)
                val verticallyClose = vGap <= 0.5f * minOf(g.height, c.height)
                if (minWidth > 0 && overlap >= 0.35f * minWidth && verticallyClose) {
                    g.add(c)
                    placed = true
                    break
                }
            }
            if (!placed) glyphs.add(Glyph(c))
        }
        if (glyphs.size < 2) return null

        // 6. Digit glyphs are the tall ones; label rows and icons are dropped.
        val glyphHeight = glyphs.maxOf { it.height }
        val digitGlyphs = glyphs.filter { it.height >= 0.6f * glyphHeight }
        if (digitGlyphs.size < 2) return null

        val bandTop = digitGlyphs.minOf { it.minY }
        val bandBottom = digitGlyphs.maxOf { it.maxY }

        // 7. Decimal-point blobs: tiny components at the baseline that are not
        //    part of any digit glyph.
        val digitParts = digitGlyphs.flatMapTo(mutableSetOf()) { it.parts }
        val maxDecimalSize = 0.30f * glyphHeight
        val decimalCandidates = components.filter {
            it !in digitParts && it.width <= maxDecimalSize && it.height <= maxDecimalSize && it.area >= 4
        }

        // 8. Classify every digit; any uncertain digit refuses the frame.
        //    (Dropping a digit silently could turn 8.7 into 7.0.)
        val readings = digitGlyphs.map { classifyGlyph(closed, gray, width, it) ?: return null }

        // 9. Attach decimals: a valid point has a digit on both sides.
        val decimalAfter = BooleanArray(digitGlyphs.size)
        var anyDecimal = false
        for (d in decimalCandidates) {
            if (d.centerY < bandTop + 0.55f * glyphHeight ||
                d.centerY > bandBottom + 0.15f * glyphHeight
            ) continue
            var bestIndex = -1
            var bestGap = Float.MAX_VALUE
            for (i in digitGlyphs.indices) {
                val g = digitGlyphs[i]
                if (g.centerX < d.centerX && d.centerX - g.centerX < 0.8f * glyphHeight) {
                    val gap = d.centerX - g.centerX
                    if (gap < bestGap) {
                        bestGap = gap
                        bestIndex = i
                    }
                }
            }
            if (bestIndex in 0 until digitGlyphs.size - 1 &&
                digitGlyphs[bestIndex + 1].centerX > d.centerX
            ) {
                decimalAfter[bestIndex] = true
                anyDecimal = true
            }
        }

        // 10. Assemble, validate range, recover a lost decimal separator.
        val raw = buildString {
            readings.forEachIndexed { i, reading ->
                append(reading.digit)
                if (decimalAfter[i]) append('.')
            }
        }

        var value = raw.toFloatOrNull()
        if (value != null && value !in MIN_GLUCOSE..MAX_GLUCOSE) {
            // mmol/L meters always show one decimal digit, so an integer above
            // the plausible range is a decimal blob we failed to detect.
            if (raw.length >= 2 && raw.all { it in '0'..'9' }) {
                value = (raw.dropLast(1) + "." + raw.last()).toFloatOrNull()
            } else {
                value = null
            }
        }
        if (value == null || value !in MIN_GLUCOSE..MAX_GLUCOSE) return null

        var confidence = readings.map { it.confidence }.average().toFloat()
        if (!anyDecimal) confidence *= NO_DECIMAL_PENALTY
        if (confidence < READING_CONFIDENCE) return null

        return Decoding(raw = raw, value = value, confidence = confidence)
    }

    private class DigitReading(val digit: Int, val confidence: Float)

    private fun classifyGlyph(
        binary: IntArray,
        gray: IntArray,
        width: Int,
        glyph: Glyph
    ): DigitReading? {
        val w = glyph.width
        val h = glyph.height
        if (h.toFloat() / maxOf(1, w) >= NARROW_ONE_ASPECT) {
            // The two right-hand bars of "1" produce a solid narrow block;
            // segment sampling cannot see a "1" shape inside it.
            return DigitReading(1, 0.95f)
        }
        val active = mutableSetOf<Segment>()
        var maxGray = 0f
        for ((segment, r) in SAMPLE_RECTS) {
            val x0 = (glyph.minX + r[0] * w).toInt()
            val y0 = (glyph.minY + r[1] * h).toInt()
            val x1 = (glyph.minX + r[2] * w).toInt()
            val y1 = (glyph.minY + r[3] * h).toInt()
            val (onRatio, grayRatio) = ratioIn(binary, gray, width, x0, y0, x1, y1)
            if (onRatio >= SEGMENT_ON_THRESHOLD) {
                active.add(segment)
                if (grayRatio > maxGray) maxGray = grayRatio
            }
        }
        if (active.isEmpty()) return null

        var bestDigit = -1
        var bestScore = Float.NEGATIVE_INFINITY
        for (digit in DIGIT_PATTERNS.indices) {
            val pattern = DIGIT_PATTERNS[digit]
            val intersection = pattern.intersect(active).size
            val missing = pattern.minus(active).size
            val extra = active.minus(pattern).size
            val score = intersection - 0.8f * missing - 0.6f * extra
            if (score > bestScore) {
                bestScore = score
                bestDigit = digit
            }
        }
        if (bestDigit < 0) return null

        val pattern = DIGIT_PATTERNS[bestDigit]
        val union = pattern.union(active).size
        var confidence = if (union == 0) 0f else pattern.intersect(active).size.toFloat() / union
        if (maxGray > 0.20f) confidence *= GRAY_PENALTY
        if (confidence < DIGIT_CONFIDENCE) return null
        return DigitReading(bestDigit, confidence)
    }

    /** Lit-pixel and weak-pixel ratios inside an absolute pixel rectangle. */
    private fun ratioIn(
        binary: IntArray,
        gray: IntArray,
        width: Int,
        rawX0: Int,
        rawY0: Int,
        rawX1: Int,
        rawY1: Int
    ): Pair<Float, Float> {
        val height = binary.size / width
        val x0 = rawX0.coerceIn(0, width - 1)
        val x1 = rawX1.coerceIn(x0 + 1, width)
        val y0 = rawY0.coerceIn(0, height - 1)
        val y1 = rawY1.coerceIn(y0 + 1, height)
        val count = (x1 - x0) * (y1 - y0)
        if (count <= 0) return 0f to 0f
        var on = 0
        var grayCount = 0
        for (y in y0 until y1) {
            val row = y * width
            for (x in x0 until x1) {
                if (binary[row + x] == 1) on++
                else if (gray[row + x] == 1) grayCount++
            }
        }
        return on.toFloat() / count to grayCount.toFloat() / count
    }

    private fun otsu(hist: IntArray, total: Int): Int {
        var sumAll = 0.0
        for (i in 0..255) sumAll += i * hist[i]
        var sumB = 0.0
        var wB = 0.0
        var bestVar = -1.0
        var bestT = 127
        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0.0) continue
            val wF = total - wB
            if (wF == 0.0) break
            sumB += t * hist[t]
            val mB = sumB / wB
            val mF = (sumAll - sumB) / wF
            val diff = mB - mF
            val v = wB * wF * diff * diff
            if (v > bestVar) {
                bestVar = v
                bestT = t
            }
        }
        return bestT
    }

    private fun closing(src: IntArray, w: Int, h: Int, r: Int): IntArray =
        erode(dilate(src, w, h, r), w, h, r)

    private fun dilate(src: IntArray, w: Int, h: Int, r: Int): IntArray {
        val tmp = IntArray(w * h)
        val dst = IntArray(w * h)
        for (y in 0 until h) {
            val base = y * w
            for (x in 0 until w) {
                var v = 0
                val lo = maxOf(0, x - r)
                val hi = minOf(w - 1, x + r)
                for (k in lo..hi) {
                    if (src[base + k] == 1) {
                        v = 1
                        break
                    }
                }
                tmp[base + x] = v
            }
        }
        for (x in 0 until w) {
            for (y in 0 until h) {
                var v = 0
                val lo = maxOf(0, y - r)
                val hi = minOf(h - 1, y + r)
                for (k in lo..hi) {
                    if (tmp[k * w + x] == 1) {
                        v = 1
                        break
                    }
                }
                dst[y * w + x] = v
            }
        }
        return dst
    }

    private fun erode(src: IntArray, w: Int, h: Int, r: Int): IntArray {
        val tmp = IntArray(w * h)
        val dst = IntArray(w * h)
        for (y in 0 until h) {
            val base = y * w
            for (x in 0 until w) {
                var v = 1
                val lo = maxOf(0, x - r)
                val hi = minOf(w - 1, x + r)
                for (k in lo..hi) {
                    if (src[base + k] == 0) {
                        v = 0
                        break
                    }
                }
                tmp[base + x] = v
            }
        }
        for (x in 0 until w) {
            for (y in 0 until h) {
                var v = 1
                val lo = maxOf(0, y - r)
                val hi = minOf(h - 1, y + r)
                for (k in lo..hi) {
                    if (tmp[k * w + x] == 0) {
                        v = 0
                        break
                    }
                }
                dst[y * w + x] = v
            }
        }
        return dst
    }

    /** 8-connectivity flood fill with an explicit IntArray stack (no boxing). */
    private fun connectedComponents(binary: IntArray, w: Int, h: Int): List<Component> {
        val labels = IntArray(w * h)
        val components = mutableListOf<Component>()
        val stack = IntArray(w * h)
        for (start in binary.indices) {
            if (binary[start] == 0 || labels[start] != 0) continue
            val component = Component()
            val id = components.size + 1
            var top = 0
            stack[top++] = start
            labels[start] = id
            while (top > 0) {
                val idx = stack[--top]
                val x = idx % w
                val y = idx / w
                component.area++
                if (x < component.minX) component.minX = x
                if (x > component.maxX) component.maxX = x
                if (y < component.minY) component.minY = y
                if (y > component.maxY) component.maxY = y
                val y0 = maxOf(0, y - 1)
                val y1 = minOf(h - 1, y + 1)
                val x0 = maxOf(0, x - 1)
                val x1 = minOf(w - 1, x + 1)
                for (ny in y0..y1) {
                    val row = ny * w
                    for (nx in x0..x1) {
                        if (nx == x && ny == y) continue
                        val n = row + nx
                        if (binary[n] == 1 && labels[n] == 0) {
                            labels[n] = id
                            stack[top++] = n
                        }
                    }
                }
            }
            components.add(component)
        }
        return components
    }
}
