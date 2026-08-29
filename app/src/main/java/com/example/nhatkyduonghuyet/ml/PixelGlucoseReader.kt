package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap
import android.graphics.RectF

data class PixelDisplayReading(
    val raw: String,
    val value: Float,
    val confidence: Float
)

enum class Segment { A, B, C, D, E, F, G }

data class SegmentPattern(
    val digit: Int,
    val on: Set<Segment>
)

data class SegmentReading(
    val digit: Int,
    val confidence: Float
)

/**
 * Reads the seven-segment glucose value directly from display pixels.
 * Pixels are sampled once from the ROI and the same array is reused for every
 * region scan, avoiding per-pixel Bitmap calls on every segment.
 */
class PixelGlucoseReader {

    private val digitPatterns = listOf(
        SegmentPattern(0, setOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.E, Segment.F)),
        SegmentPattern(1, setOf(Segment.B, Segment.C)),
        SegmentPattern(2, setOf(Segment.A, Segment.B, Segment.G, Segment.E, Segment.D)),
        SegmentPattern(3, setOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.G)),
        SegmentPattern(4, setOf(Segment.F, Segment.G, Segment.B, Segment.C)),
        SegmentPattern(5, setOf(Segment.A, Segment.F, Segment.G, Segment.C, Segment.D)),
        SegmentPattern(6, setOf(Segment.A, Segment.F, Segment.G, Segment.E, Segment.C, Segment.D)),
        SegmentPattern(7, setOf(Segment.A, Segment.B, Segment.C)),
        SegmentPattern(8, Segment.values().toSet()),
        SegmentPattern(9, setOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.F, Segment.G))
    )

    /**
     * Processes the cropped ROI of the glucose meter display.
     * Expects the image to be normalized (upright, centered on digits).
     */
    fun processDisplay(roi: Bitmap): PixelDisplayReading? {
        val width = roi.width
        val height = roi.height
        val pixels = IntArray(width * height)
        roi.getPixels(pixels, 0, width, 0, 0, width, height)

        // Divide ROI into 3 potential digit cells and 1 decimal area.
        // These proportions are based on On Call Plus layout.
        val digitCells = listOf(
            // Digit 1 (Tens) - might be empty
            RectF(0.05f, 0.15f, 0.32f, 0.85f),
            // Digit 2 (Units)
            RectF(0.35f, 0.15f, 0.62f, 0.85f),
            // Digit 3 (Decimals)
            RectF(0.68f, 0.15f, 0.95f, 0.85f)
        )

        val readings = mutableListOf<SegmentReading>()
        for (cellRect in digitCells) {
            val reading = readDigit(pixels, width, height, cellRect)
            if (reading != null) {
                readings.add(reading)
            }
        }

        // Detect decimal point between digit 2 and 3.
        val decimalDetected = darkPixelRatio(
            pixels, width, height,
            RectF(0.62f, 0.75f, 0.68f, 0.90f)
        ) > DECIMAL_POINT_THRESHOLD

        return combineDigits(readings, decimalDetected)
    }

    private fun readDigit(pixels: IntArray, width: Int, height: Int, cell: RectF): SegmentReading? {
        val active = mutableSetOf<Segment>()
        // Improved segment proportions based on seven-segment display standards.
        val segments = mapOf(
            Segment.A to RectF(0.20f, 0.08f, 0.80f, 0.18f),
            Segment.B to RectF(0.82f, 0.15f, 0.95f, 0.45f),
            Segment.C to RectF(0.82f, 0.55f, 0.95f, 0.85f),
            Segment.D to RectF(0.20f, 0.82f, 0.80f, 0.92f),
            Segment.E to RectF(0.05f, 0.55f, 0.18f, 0.85f),
            Segment.F to RectF(0.05f, 0.15f, 0.18f, 0.45f),
            Segment.G to RectF(0.20f, 0.45f, 0.80f, 0.55f)
        )

        for ((seg, rect) in segments) {
            // Segment rects are normalized within the cell; map them onto the ROI.
            val cellWidth = cell.right - cell.left
            val cellHeight = cell.bottom - cell.top
            val absRect = RectF(
                cell.left + rect.left * cellWidth,
                cell.top + rect.top * cellHeight,
                cell.left + rect.right * cellWidth,
                cell.top + rect.bottom * cellHeight
            )
            if (darkPixelRatio(pixels, width, height, absRect) >= SEGMENT_ON_THRESHOLD) {
                active.add(seg)
            }
        }

        if (active.isEmpty()) return null

        val best = digitPatterns.maxByOrNull { pattern ->
            val intersection = pattern.on.intersect(active).size
            val missing = pattern.on.minus(active).size
            val extra = active.minus(pattern.on).size
            intersection * 1.0f - missing * 0.8f - extra * 0.6f
        } ?: return null

        val confidence = calculateConfidence(best.on, active)
        return if (confidence >= PIXEL_DIGIT_CONFIDENCE) {
            SegmentReading(best.digit, confidence)
        } else {
            null
        }
    }

    /**
     * Dark-pixel ratio inside a normalized rectangle of the pixel array.
     * Uses the local average luminance as an adaptive threshold so dark
     * seven-segment elements are recognized on both light and dark displays.
     */
    private fun darkPixelRatio(pixels: IntArray, width: Int, height: Int, rect: RectF): Float {
        val left = (width * rect.left).toInt().coerceIn(0, width - 1)
        val top = (height * rect.top).toInt().coerceIn(0, height - 1)
        val right = (width * rect.right).toInt().coerceIn(left + 1, width)
        val bottom = (height * rect.bottom).toInt().coerceIn(top + 1, height)

        val sampleCount = (right - left) * (bottom - top)
        if (sampleCount <= 0) return 0f

        var totalLuminance = 0.0
        for (y in top until bottom) {
            val rowOffset = y * width
            for (x in left until right) {
                totalLuminance += luminance(pixels[rowOffset + x])
            }
        }
        val threshold = (totalLuminance / sampleCount * 0.85).coerceIn(40.0, 180.0)

        var darkCount = 0
        for (y in top until bottom) {
            val rowOffset = y * width
            for (x in left until right) {
                if (luminance(pixels[rowOffset + x]) < threshold) {
                    darkCount++
                }
            }
        }

        return darkCount.toFloat() / sampleCount
    }

    private fun calculateConfidence(expected: Set<Segment>, actual: Set<Segment>): Float {
        val correct = expected.intersect(actual).size
        val total = expected.union(actual).size
        return if (total == 0) 0f else correct.toFloat() / total
    }

    private fun combineDigits(digits: List<SegmentReading>, decimalDetected: Boolean): PixelDisplayReading? {
        if (digits.isEmpty()) return null

        val text = buildString {
            digits.forEachIndexed { index, reading ->
                // For On Call Plus, decimal is usually before the last digit if there are 2 or 3 digits.
                if (decimalDetected && index == digits.size - 1) {
                    append('.')
                }
                append(reading.digit)
            }
        }

        val value = text.toFloatOrNull() ?: return null
        if (value !in MIN_GLUCOSE..MAX_GLUCOSE) return null

        val confidence = digits.map { it.confidence }.average().toFloat()
        return if (confidence >= PIXEL_READING_CONFIDENCE) {
            PixelDisplayReading(text, value, confidence)
        } else {
            null
        }
    }

    private inline fun luminance(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return r * 0.299f + g * 0.587f + b * 0.114f
    }
}