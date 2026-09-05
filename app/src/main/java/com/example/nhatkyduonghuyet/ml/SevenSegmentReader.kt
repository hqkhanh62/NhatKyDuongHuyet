package com.example.nhatkyduonghuyet.ml

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A seven-segment reading recovered from display pixels.
 *
 * @param margin distance (in "segments") between the winning digit pattern and the runner-up
 * for the least certain digit. It is a real separability measure, unlike a similarity ratio
 * which stays high even for garbage input.
 */
data class SegmentedReading(
    val text: String,
    val value: Float,
    val margin: Float,
    val digitCount: Int
)

/**
 * Reads a seven-segment number (glucose meter LCD) straight from pixels.
 *
 * Deliberately free of Android dependencies so the whole algorithm is covered by JVM unit
 * tests — the previous implementation could only run on a device, which is why a broken
 * thresholding rule stayed unnoticed.
 *
 * Pipeline:
 *  1. Otsu threshold over the **whole** ROI (never per segment window).
 *  2. Polarity auto-detection, so inverted (light-on-dark) displays work too.
 *  3. Stroke-width estimation → morphological closing that bridges the gaps *inside* a glyph
 *     without merging neighbouring glyphs.
 *  4. Shear (italic) estimation, since most meters use slanted digits.
 *  5. Digit localisation by column projection of the **upper** part of the band, so the
 *     decimal point can never distort digit boxes; fixed-pitch splitting repairs glyphs that
 *     were merged by the closing step.
 *  6. Per-digit classification by sampling the seven segment windows of the *measured* box.
 *  7. Geometry sanity checks (even cell pitch, nothing clipped or dropped) and a margin gate.
 *
 * Any doubt results in `null`: for a medical value, "cannot read" is acceptable, "read wrong"
 * is not.
 */
class SevenSegmentReader(private val config: Config = Config()) {

    data class Config(
        /** Ratio at which a segment window counts as fully lit. */
        val segmentOn: Float = 0.40f,
        /** Ratio below which a segment window counts as fully dark. */
        val segmentOff: Float = 0.15f,
        /** Minimum separation (in segments) between the best and the second-best digit. */
        val minMargin: Float = 0.55f,
        /** Minimum height of the digit band relative to the ROI height. */
        val minBandHeightRatio: Float = 0.30f,
        /** Maximum number of digits accepted on a meter display. */
        val maxDigits: Int = 4,
        /**
         * Reject readings without a visible decimal point. Every mmol/L meter prints one, so
         * this rules out integer displays (mg/dL, memory counters, dates) being read as mmol.
         */
        val requireDecimalPoint: Boolean = true
    )

    /**
     * @param luma row-major luminance values in 0..255, [width] * [height] entries.
     * @return the reading, or null when the ROI cannot be decoded with confidence.
     */
    fun read(luma: IntArray, width: Int, height: Int): SegmentedReading? {
        if (width < MIN_SIDE || height < MIN_SIDE) return null
        if (luma.size < width * height) return null

        var mask = ImageOps.binarize(luma, width * height)
        val inkRatio = ImageOps.inkRatio(mask)
        if (inkRatio < MIN_INK_RATIO || inkRatio > MAX_INK_RATIO) return null

        if (touchesRoiBorder(mask, width, height)) return null

        val stroke = ImageOps.strokeWidth(mask, width, height)
        if (stroke < 2) return null
        mask = ImageOps.close(mask, width, height, max(1, (CLOSE_RADIUS * stroke).roundToInt()))

        val components = findComponents(mask, width, height, max(4, stroke * stroke / 2))
        if (components.isEmpty()) return null

        val tallest = components.maxOf { it.height }
        val glyphs = components.filter { it.height >= 0.5f * tallest }
        if (glyphs.isEmpty()) return null

        val bandTop = glyphs.minOf { it.top }
        val bandBottom = glyphs.maxOf { it.bottom }
        val bandLeft = glyphs.minOf { it.left }
        val bandRight = glyphs.maxOf { it.right }
        val bandHeight = bandBottom - bandTop
        if (bandHeight < config.minBandHeightRatio * height) return null
        // Digits touching the ROI border mean the display is not fully framed.
        if (bandLeft <= 1 || bandRight >= width - 1 || bandTop <= 1 || bandBottom >= height - 1) {
            return null
        }

        val upperOccupancy = BooleanArray(width)
        val boxes = locateDigits(mask, width, bandTop, bandBottom, bandHeight, stroke, upperOccupancy)
            ?: return null

        val fit = fitDigits(mask, width, boxes, bandHeight, stroke) ?: return null
        val digits = fit.digits
        val fittedShear = fit.shear
        if (digits.isEmpty()) return null

        // Every seven-segment glyph lights B or C, so right edges mark the cell grid.
        var cellPitch = (digits[0].box.right - digits[0].box.left).toFloat()
        if (digits.size >= 2) {
            val steps = IntArray(digits.size - 1) { digits[it + 1].box.right - digits[it].box.right }
            val minStep = steps.min()
            val maxStep = steps.max()
            if (minStep <= 0 || maxStep > PITCH_TOLERANCE * minStep) return null
            cellPitch = steps.sorted()[steps.size / 2].toFloat()
            if (cellPitch < 0.5f * bandHeight || cellPitch > 1.6f * bandHeight) return null
        }
        // A digit dropped at either end would leave unexplained ink in the band.
        if (digits.first().box.left - bandLeft > 0.45f * cellPitch) return null
        if (bandRight - digits.last().box.right > 0.45f * cellPitch) return null

        val hasDecimalPoint = hasDecimalPoint(
            mask, width, upperOccupancy, digits, bandTop, bandBottom, stroke
        )
        if (config.requireDecimalPoint && !hasDecimalPoint) return null

        // A mmol/L meter always shows exactly one decimal (5.7, 12.3), so the position of the
        // point follows from the digit count. Only its presence has to be detected, which is
        // far more robust than deciding which inter-digit gap contains it.
        val builder = StringBuilder()
        digits.forEachIndexed { index, digit ->
            if (hasDecimalPoint && digits.size > 1 && index == digits.size - 1) builder.append('.')
            builder.append(digit.value)
        }
        val text = builder.toString()
        val integerPart = text.substringBefore('.')
        if (integerPart.length > 1 && integerPart[0] == '0') return null
        val value = text.toFloatOrNull() ?: return null

        return SegmentedReading(
            text = text,
            value = value,
            margin = digits.minOf { it.margin },
            digitCount = digits.size
        )
    }

    // ------------------------------------------------------------------ internals

    private data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private data class Digit(
        val box: Box,
        val value: Int,
        val margin: Float,
        val score: Float = MAX_MARGIN
    )

    private class Component(
        var left: Int,
        var top: Int,
        var right: Int,
        var bottom: Int,
        var area: Int
    ) {
        val height: Int get() = bottom - top
    }


    /**
     * True when ink runs along the ROI border: the display is either clipped by the guide
     * frame or the meter bezel is inside the crop. Both make a reading unreliable.
     */
    private fun touchesRoiBorder(mask: BooleanArray, width: Int, height: Int): Boolean {
        val columnLimit = (BORDER_INK_RATIO * height).toInt()
        val rowLimit = (BORDER_INK_RATIO * width).toInt()
        for (x in intArrayOf(0, 1, width - 2, width - 1)) {
            var count = 0
            for (y in 0 until height) if (mask[y * width + x]) count++
            if (count > columnLimit) return true
        }
        for (y in intArrayOf(0, 1, height - 2, height - 1)) {
            var count = 0
            val row = y * width
            for (x in 0 until width) if (mask[row + x]) count++
            if (count > rowLimit) return true
        }
        return false
    }





    /** Eight-connected labelling with an explicit stack (no recursion). */
    private fun findComponents(
        mask: BooleanArray,
        width: Int,
        height: Int,
        minArea: Int
    ): List<Component> {
        val visited = BooleanArray(mask.size)
        val stack = IntArray(mask.size)
        val result = ArrayList<Component>()
        for (start in mask.indices) {
            if (!mask[start] || visited[start]) continue
            var stackSize = 0
            stack[stackSize++] = start
            visited[start] = true
            val sy = start / width
            val sx = start % width
            val component = Component(sx, sy, sx + 1, sy + 1, 0)
            while (stackSize > 0) {
                val index = stack[--stackSize]
                val y = index / width
                val x = index % width
                component.area++
                if (x < component.left) component.left = x
                if (x + 1 > component.right) component.right = x + 1
                if (y < component.top) component.top = y
                if (y + 1 > component.bottom) component.bottom = y + 1
                for (dy in -1..1) {
                    val ny = y + dy
                    if (ny < 0 || ny >= height) continue
                    for (dx in -1..1) {
                        val nx = x + dx
                        if (nx < 0 || nx >= width) continue
                        val n = ny * width + nx
                        if (mask[n] && !visited[n]) {
                            visited[n] = true
                            stack[stackSize++] = n
                        }
                    }
                }
            }
            if (component.area >= minArea) result += component
        }
        return result
    }

    /**
     * Column projection of the upper part of the band (never the baseline, where the decimal
     * point lives), then fixed-pitch repair of glyphs merged by the closing step.
     */
    private fun locateDigits(
        mask: BooleanArray,
        width: Int,
        bandTop: Int,
        bandBottom: Int,
        bandHeight: Int,
        stroke: Int,
        upperOccupancy: BooleanArray
    ): List<Box>? {
        val upperBottom = min(bandBottom, bandTop + max(2, (UPPER_BAND * bandHeight).toInt()))
        val occupied = upperOccupancy
        for (y in bandTop until upperBottom) {
            val row = y * width
            for (x in 0 until width) {
                if (mask[row + x]) occupied[x] = true
            }
        }

        val runs = ArrayList<IntArray>()
        var start = -1
        for (x in 0 until width) {
            if (occupied[x] && start < 0) {
                start = x
            } else if (!occupied[x] && start >= 0) {
                runs += intArrayOf(start, x)
                start = -1
            }
        }
        if (start >= 0) runs += intArrayOf(start, width)
        if (runs.isEmpty()) return null

        val tinyGap = max(1, (0.04f * bandHeight).toInt())
        val merged = ArrayList<IntArray>()
        for (run in runs) {
            val last = merged.lastOrNull()
            if (last != null && run[0] - last[1] <= tinyGap) {
                last[1] = run[1]
            } else {
                merged += intArrayOf(run[0], run[1])
            }
        }
        val minRunWidth = max(2, stroke / 2)
        merged.retainAll { it[1] - it[0] >= minRunWidth }
        if (merged.isEmpty()) return null

        val wide = merged.map { it[1] - it[0] }.filter { it >= NARROW_DIGIT_RATIO * bandHeight }.sorted()
        val pitch = if (wide.isNotEmpty()) wide[wide.size / 2] else merged.maxOf { it[1] - it[0] }
        if (pitch <= 0) return null

        val boxes = ArrayList<Box>()
        for (run in merged) {
            val runWidth = run[1] - run[0]
            val parts = (runWidth.toFloat() / pitch).roundToInt().coerceIn(1, config.maxDigits)
            if (runWidth > 1.35f * pitch && parts >= 2) {
                val step = runWidth.toFloat() / parts
                for (i in 0 until parts) {
                    boxes += Box(
                        left = run[0] + (i * step).toInt(),
                        top = bandTop,
                        right = run[0] + ((i + 1) * step).toInt(),
                        bottom = bandBottom
                    )
                }
            } else {
                boxes += Box(run[0], bandTop, run[1], bandBottom)
            }
        }
        if (boxes.isEmpty() || boxes.size > config.maxDigits) return null

        // Cells on a seven-segment display are equally wide. Boxes of clearly different
        // widths mean two glyphs were glued together (or one was cut in half), so the
        // reading cannot be trusted.
        val fullWidths = boxes.map { it.right - it.left }
            .filter { it >= NARROW_DIGIT_RATIO * bandHeight }
        if (fullWidths.size >= 2 && fullWidths.max() > PITCH_TOLERANCE * fullWidths.min()) {
            return null
        }
        return boxes
    }

    private fun fillRatio(mask: BooleanArray, width: Int, box: Box): Float {
        var count = 0
        var total = 0
        for (y in box.top until box.bottom) {
            val row = y * width
            for (x in box.left until box.right) {
                total++
                if (mask[row + x]) count++
            }
        }
        return if (total == 0) 0f else count.toFloat() / total
    }

    private class Fit(val digits: List<Digit>, val shear: Float)

    /**
     * Meter displays are usually italic. Rather than warping the image (which distorts the
     * glyph boxes), the reading is decoded under a handful of candidate slants and the slant
     * whose digits are best explained by the seven-segment model wins.
     */
    private fun fitDigits(
        mask: BooleanArray,
        width: Int,
        boxes: List<Box>,
        bandHeight: Int,
        stroke: Int
    ): Fit? {
        var best: Fit? = null
        var bestScore = -1f
        for (shear in SHEAR_CANDIDATES) {
            var total = 0f
            var usable = true
            val digits = ArrayList<Digit>(boxes.size)
            for (box in boxes) {
                val boxWidth = glyphSpan(box, shear)[1]
                if (boxWidth < NARROW_DIGIT_RATIO * bandHeight) {
                    // Narrow, solid, full-height bar: the only glyph shaped like a '1'.
                    if (boxWidth < max(2f, 0.5f * stroke) || fillRatio(mask, width, box) < MIN_ONE_FILL) {
                        usable = false
                        break
                    }
                    digits += Digit(box, 1, MAX_MARGIN)
                    continue
                }
                if (boxWidth > WIDE_DIGIT_RATIO * bandHeight) {
                    usable = false
                    break
                }
                val digit = classify(mask, width, box, shear)
                if (digit == null || digit.margin < config.minMargin) {
                    usable = false
                    break
                }
                digits += digit
                total += digit.score
            }
            if (!usable || digits.isEmpty()) continue
            if (total > bestScore) {
                bestScore = total
                best = Fit(digits, shear)
            }
        }
        return best
    }

    /** Horizontal offset of the glyph at row [y] for a given slant. */
    private fun slantOffset(box: Box, y: Int, shear: Float): Int =
        (shear * ((box.top + box.bottom) / 2f - y)).roundToInt()

    /**
     * Maps a measured bounding box back to the upright glyph it contains.
     *
     * Digit boxes are measured on the projection of the band's upper part only (the decimal
     * point must not widen them), so for a slanted glyph the box spans the offsets seen over
     * those rows — not the whole glyph height.
     */
    private fun glyphSpan(box: Box, shear: Float): FloatArray {
        val height = (box.bottom - box.top).toFloat()
        val offsetAtTop = shear * (height / 2f)
        val offsetAtProjectionBottom = shear * (height / 2f - UPPER_BAND * height)
        val low = min(offsetAtTop, offsetAtProjectionBottom)
        val high = max(offsetAtTop, offsetAtProjectionBottom)
        val left = box.left - low
        val glyphWidth = (box.right - box.left) - (high - low)
        return floatArrayOf(left, glyphWidth)
    }

    private fun segmentRatio(
        mask: BooleanArray,
        width: Int,
        box: Box,
        window: FloatArray,
        shear: Float
    ): Float {
        val boxHeight = box.bottom - box.top
        val span = glyphSpan(box, shear)
        val glyphLeft = span[0]
        val glyphWidth = span[1]
        if (glyphWidth <= 2f) return 0f

        val top = box.top + (boxHeight * window[1]).toInt()
        val bottom = max(top + 1, box.top + Math.ceil((boxHeight * window[3]).toDouble()).toInt())
        var count = 0
        var total = 0
        for (y in top until min(bottom, box.bottom)) {
            val shift = slantOffset(box, y, shear)
            val left = (glyphLeft + glyphWidth * window[0]).toInt() + shift
            val right = max(left + 1, Math.ceil((glyphLeft + glyphWidth * window[2]).toDouble()).toInt() + shift)
            val row = y * width
            for (x in left until right) {
                if (x < 0 || x >= width) continue
                total++
                if (mask[row + x]) count++
            }
        }
        return if (total == 0) 0f else count.toFloat() / total
    }

    private fun classify(mask: BooleanArray, width: Int, box: Box, shear: Float): Digit? {
        val lit = FloatArray(SEGMENT_COUNT)
        for (s in 0 until SEGMENT_COUNT) {
            val ratio = segmentRatio(mask, width, box, SEGMENT_WINDOWS[s], shear)
            lit[s] = ((ratio - config.segmentOff) / (config.segmentOn - config.segmentOff))
                .coerceIn(0f, 1f)
        }
        var bestScore = -1f
        var secondScore = -1f
        var bestDigit = -1
        for (digit in 0..9) {
            val pattern = DIGIT_PATTERNS[digit]
            var score = 0f
            for (s in 0 until SEGMENT_COUNT) {
                val expected = if (pattern[s]) 1f else 0f
                score += 1f - abs(expected - lit[s])
            }
            if (score > bestScore) {
                secondScore = bestScore
                bestScore = score
                bestDigit = digit
            } else if (score > secondScore) {
                secondScore = score
            }
        }
        if (bestDigit < 0) return null
        return Digit(box, bestDigit, bestScore - secondScore, bestScore)
    }

    /**
     * True when a decimal point sits on the baseline between two cells.
     *
     * The dot is found where a column carries ink at the baseline but none in the upper part
     * of the band. That test needs no slant correction: a slanted dot and the glyph above it
     * move in opposite directions, which only makes the signature clearer. Shape and size
     * limits keep a segment leaking into the gap from being mistaken for a point.
     */
    private fun hasDecimalPoint(
        mask: BooleanArray,
        width: Int,
        upperOccupancy: BooleanArray,
        digits: List<Digit>,
        bandTop: Int,
        bandBottom: Int,
        stroke: Int
    ): Boolean {
        if (digits.size < 2) return false
        val bandHeight = bandBottom - bandTop
        val stripTop = max(bandTop, bandBottom - (DOT_STRIP * bandHeight).toInt())
        val searchLeft = digits.first().box.left
        val searchRight = digits.last().box.right
        val minArea = max(4, (0.12f * stroke * stroke).toInt())
        val minWidth = max(2, (0.35f * stroke).toInt())
        val maxWidth = max(minWidth + 1, (0.45f * bandHeight).toInt())

        var runStart = -1
        var x = searchLeft
        while (x <= searchRight) {
            val candidate = x < width && !upperOccupancy[x] && columnHasInk(mask, width, x, stripTop, bandBottom)
            if (candidate && runStart < 0) {
                runStart = x
            } else if (!candidate && runStart >= 0) {
                if (isDecimalPoint(mask, width, runStart, x, bandTop, bandBottom, minArea, minWidth, maxWidth)) {
                    return true
                }
                runStart = -1
            }
            x++
        }
        return runStart >= 0 && isDecimalPoint(
            mask, width, runStart, searchRight, bandTop, bandBottom, minArea, minWidth, maxWidth
        )
    }

    private fun columnHasInk(
        mask: BooleanArray,
        width: Int,
        x: Int,
        top: Int,
        bottom: Int
    ): Boolean {
        for (y in top until bottom) if (mask[y * width + x]) return true
        return false
    }

    private fun isDecimalPoint(
        mask: BooleanArray,
        width: Int,
        left: Int,
        right: Int,
        bandTop: Int,
        bandBottom: Int,
        minArea: Int,
        minWidth: Int,
        maxWidth: Int
    ): Boolean {
        val runWidth = right - left
        if (runWidth < minWidth || runWidth > maxWidth) return false
        val bandHeight = bandBottom - bandTop
        var count = 0
        var inkTop = bandBottom
        var inkBottom = bandTop
        for (y in bandTop until bandBottom) {
            val row = y * width
            for (x in left until right) {
                if (!mask[row + x]) continue
                count++
                if (y < inkTop) inkTop = y
                if (y + 1 > inkBottom) inkBottom = y + 1
            }
        }
        if (count < minArea) return false
        if (inkBottom - inkTop > DOT_STRIP * bandHeight) return false
        if (inkBottom < bandBottom - DOT_BASELINE * bandHeight) return false
        return true
    }

    private companion object {
        const val MIN_SIDE = 24
        const val MIN_INK_RATIO = 0.01f
        const val MAX_INK_RATIO = 0.45f
        const val SEGMENT_COUNT = 7
        const val MAX_MARGIN = 7f
        const val NARROW_DIGIT_RATIO = 0.34f
        const val WIDE_DIGIT_RATIO = 1.1f
        const val MIN_ONE_FILL = 0.35f
        const val PITCH_TOLERANCE = 1.35f
        const val UPPER_BAND = 0.70f
        const val DOT_STRIP = 0.40f
        /** How far above the baseline the bottom of the dot may sit. */
        const val DOT_BASELINE = 0.28f
        /** Closing radius as a fraction of the stroke width. */
        const val CLOSE_RADIUS = 0.35f

        /** Ink along the ROI border above this fraction means the display is clipped. */
        const val BORDER_INK_RATIO = 0.12f

        /** Candidate italic slants (dx per unit of glyph height), upright first so that a
         *  slant is only chosen when it explains the glyphs strictly better. */
        val SHEAR_CANDIDATES = floatArrayOf(
            0f, 0.04f, -0.04f, 0.08f, -0.08f, 0.12f, -0.12f,
            0.16f, -0.16f, 0.20f, -0.20f, 0.24f, -0.24f
        )

        /** Segment windows inside a digit box: left, top, right, bottom (normalised). */
        val SEGMENT_WINDOWS = arrayOf(
            floatArrayOf(0.22f, 0.00f, 0.78f, 0.16f), // A top
            floatArrayOf(0.68f, 0.08f, 1.00f, 0.42f), // B top right
            floatArrayOf(0.68f, 0.58f, 1.00f, 0.92f), // C bottom right
            floatArrayOf(0.22f, 0.84f, 0.78f, 1.00f), // D bottom
            floatArrayOf(0.00f, 0.58f, 0.32f, 0.92f), // E bottom left
            floatArrayOf(0.00f, 0.08f, 0.32f, 0.42f), // F top left
            floatArrayOf(0.22f, 0.42f, 0.78f, 0.58f)  // G middle
        )

        /** Lit segments per digit, indexed A,B,C,D,E,F,G. */
        val DIGIT_PATTERNS = arrayOf(
            booleanArrayOf(true, true, true, true, true, true, false),    // 0
            booleanArrayOf(false, true, true, false, false, false, false), // 1
            booleanArrayOf(true, true, false, true, true, false, true),   // 2
            booleanArrayOf(true, true, true, true, false, false, true),   // 3
            booleanArrayOf(false, true, true, false, false, true, true),  // 4
            booleanArrayOf(true, false, true, true, false, true, true),   // 5
            booleanArrayOf(true, false, true, true, true, true, true),    // 6
            booleanArrayOf(true, true, true, false, false, false, false), // 7
            booleanArrayOf(true, true, true, true, true, true, true),     // 8
            booleanArrayOf(true, true, true, true, false, true, true)     // 9
        )
    }
}
