package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

object ImageUtils {

    /** Width in px of the bitmap handed to ML Kit after enhancement. */
    const val OCR_TARGET_WIDTH = 1280

    /**
     * Fallback ROI for the main glucose value on the On Call Plus display.
     * Camera callers pass the ROI derived from the on-screen green frame so the
     * analysis matches exactly what the user framed.
     */
    val DISPLAY_ROI = NormalizedRect(
        left = 0.20f,
        top = 0.30f,
        right = 0.80f,
        bottom = 0.70f
    )

    data class NormalizedRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return source
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun cropNormalized(source: Bitmap, roi: NormalizedRect): Bitmap {
        val left = (source.width * roi.left).toInt().coerceIn(0, source.width - 1)
        val top = (source.height * roi.top).toInt().coerceIn(0, source.height - 1)
        val right = (source.width * roi.right).toInt().coerceIn(left + 1, source.width)
        val bottom = (source.height * roi.bottom).toInt().coerceIn(top + 1, source.height)

        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    /**
     * Prepares a cropped meter-display bitmap for ML Kit OCR:
     * grayscale conversion, percentile contrast stretching and upscaling.
     *
     * Low-contrast LCD digits become large, high-contrast glyphs which text
     * recognition reads far more reliably, especially after the display crop
     * is taken from a handheld camera frame.
     */
    fun enhanceForOcr(source: Bitmap, targetWidth: Int = OCR_TARGET_WIDTH): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val luminance = IntArray(pixels.size)
        val histogram = IntArray(LUMINANCE_LEVELS)
        var index = 0
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luma = (r * 299 + g * 587 + b * 114) / 1000
            luminance[index++] = luma
            histogram[luma]++
        }

        // Percentile stretch: ignore the darkest/brightest tails so small
        // glare spots or shadows do not compress the useful contrast range.
        val total = pixels.size
        val lowCut = (total * CONTRAST_CUT_LOW).toInt()
        val highCut = (total * CONTRAST_CUT_HIGH).toInt()
        var accumulator = 0
        var low = 0
        for (value in 0 until LUMINANCE_LEVELS) {
            accumulator += histogram[value]
            if (accumulator >= lowCut) {
                low = value
                break
            }
        }
        accumulator = 0
        var high = LUMINANCE_LEVELS - 1
        for (value in 0 until LUMINANCE_LEVELS) {
            accumulator += histogram[value]
            if (accumulator >= highCut) {
                high = value
                break
            }
        }
        if (high - low < MIN_CONTRAST_RANGE) {
            high = (low + MIN_CONTRAST_RANGE).coerceAtMost(LUMINANCE_LEVELS - 1)
        }
        val range = (high - low).coerceAtLeast(1).toFloat()

        for (i in pixels.indices) {
            val stretched = ((luminance[i] - low) / range * 255f)
                .coerceIn(0f, 255f)
                .toInt()
            val gray = (0xFF shl 24) or (stretched shl 16) or (stretched shl 8) or stretched
            pixels[i] = gray
        }

        val grayBitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        if (width == targetWidth) return grayBitmap

        val scaledHeight = (height.toLong() * targetWidth / width)
            .coerceAtLeast(1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        return Bitmap.createScaledBitmap(grayBitmap, targetWidth, scaledHeight, true)
    }

    private const val LUMINANCE_LEVELS = 256
    private const val CONTRAST_CUT_LOW = 0.02f
    private const val CONTRAST_CUT_HIGH = 0.98f
    private const val MIN_CONTRAST_RANGE = 24
}
