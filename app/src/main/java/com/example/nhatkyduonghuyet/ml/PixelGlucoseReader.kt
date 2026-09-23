package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap

data class PixelDisplayReading(
    val raw: String,
    val value: Float,
    val confidence: Float
)

/**
 * Reads the seven-segment glucose value from a cropped, contrast-enhanced
 * bitmap of the meter display.
 *
 * All the actual work happens in [SevenSegmentDecoder] (connected-component
 * based, unit-testable on the JVM); this class only adapts android Bitmaps
 * and keeps the analysis crop small enough for a 4 fps loop.
 */
class PixelGlucoseReader {

    /** Analysis crops wider than this are downscaled: enough detail for the
     *  segment classifier, a fraction of the pixel work. */
    private val maxAnalysisWidth = 640

    fun processDisplay(roi: Bitmap): PixelDisplayReading? {
        if (roi.width <= 0 || roi.height <= 0) return null
        val source = if (roi.width > maxAnalysisWidth) {
            val scale = maxAnalysisWidth.toFloat() / roi.width
            Bitmap.createScaledBitmap(
                roi,
                maxAnalysisWidth,
                (roi.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            roi
        }
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return null
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        return SevenSegmentDecoder.decode(pixels, width, height)?.let {
            PixelDisplayReading(raw = it.raw, value = it.value, confidence = it.confidence)
        }
    }
}
