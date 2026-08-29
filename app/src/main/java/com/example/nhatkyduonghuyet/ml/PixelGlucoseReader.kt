package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import kotlin.math.abs

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
        
        // Convert to grayscale and apply adaptive thresholding (simplified)
        val pixels = IntArray(width * height)
        roi.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val gray = ByteArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = ((r * 0.299 + g * 0.587 + b * 0.114).toInt()).toByte()
        }

        // Divide ROI into 3 potential digit cells and 1 decimal area
        // These proportions are based on On Call Plus layout
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
            val cellBitmap = cropRect(roi, cellRect)
            val reading = readDigit(cellBitmap)
            if (reading != null) {
                readings.add(reading)
            }
        }

        // Detect decimal point between digit 2 and 3
        val decimalDetected = detectDecimalPoint(roi, RectF(0.62f, 0.75f, 0.68f, 0.90f))

        return combineDigits(readings, decimalDetected)
    }

    private fun readDigit(cell: Bitmap): SegmentReading? {
        val active = mutableSetOf<Segment>()
        val segments = mapOf(
            Segment.A to RectF(0.2f, 0.05f, 0.8f, 0.15f),
            Segment.B to RectF(0.85f, 0.15f, 0.95f, 0.45f),
            Segment.C to RectF(0.85f, 0.55f, 0.95f, 0.85f),
            Segment.D to RectF(0.2f, 0.85f, 0.8f, 0.95f),
            Segment.E to RectF(0.05f, 0.55f, 0.15f, 0.85f),
            Segment.F to RectF(0.05f, 0.15f, 0.15f, 0.45f),
            Segment.G to RectF(0.2f, 0.45f, 0.8f, 0.55f)
        )

        for ((seg, rect) in segments) {
            if (darkPixelRatio(cell, rect) >= 0.28f) {
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
        return if (confidence >= 0.70f) {
            SegmentReading(best.digit, confidence)
        } else {
            null
        }
    }

    private fun darkPixelRatio(bitmap: Bitmap, rect: RectF): Float {
        val left = (bitmap.width * rect.left).toInt().coerceIn(0, bitmap.width - 1)
        val top = (bitmap.height * rect.top).toInt().coerceIn(0, bitmap.height - 1)
        val right = (bitmap.width * rect.right).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (bitmap.height * rect.bottom).toInt().coerceIn(top + 1, bitmap.height)

        var darkCount = 0
        var totalCount = 0
        
        // Thresholding: use a simple one for now
        for (y in top until bottom) {
            for (x in left until right) {
                val pixel = bitmap.getPixel(x, y)
                val luminance = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114)
                if (luminance < 100) { // Dark pixel
                    darkCount++
                }
                totalCount++
            }
        }
        
        return if (totalCount == 0) 0f else darkCount.toFloat() / totalCount
    }

    private fun calculateConfidence(expected: Set<Segment>, actual: Set<Segment>): Float {
        val correct = expected.intersect(actual).size
        val total = expected.union(actual).size
        return if (total == 0) 0f else correct.toFloat() / total
    }

    private fun detectDecimalPoint(roi: Bitmap, rect: RectF): Boolean {
        return darkPixelRatio(roi, rect) > 0.25f
    }

    private fun combineDigits(digits: List<SegmentReading>, decimalDetected: Boolean): PixelDisplayReading? {
        if (digits.isEmpty()) return null

        val text = buildString {
            digits.forEachIndexed { index, reading ->
                // For On Call Plus, decimal is usually before the last digit if there are 2 or 3 digits
                if (decimalDetected && index == digits.size - 1) {
                    append('.')
                }
                append(reading.digit)
            }
        }

        val value = text.toFloatOrNull() ?: return null
        if (value !in 2.0f..30.0f) return null

        val confidence = digits.map { it.confidence }.average().toFloat()
        return if (confidence >= 0.78f) {
            PixelDisplayReading(text, value, confidence)
        } else {
            null
        }
    }

    private fun cropRect(source: Bitmap, rect: RectF): Bitmap {
        val left = (source.width * rect.left).toInt().coerceIn(0, source.width - 1)
        val top = (source.height * rect.top).toInt().coerceIn(0, source.height - 1)
        val right = (source.width * rect.right).toInt().coerceIn(left + 1, source.width)
        val bottom = (source.height * rect.bottom).toInt().coerceIn(top + 1, source.height)
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }
}
