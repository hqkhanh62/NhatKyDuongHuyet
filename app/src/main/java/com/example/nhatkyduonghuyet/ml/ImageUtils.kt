package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.max

/**
 * Android glue around the pure image pipeline: bitmap ↔ luminance conversion, cropping and
 * the pre-processing that makes seven-segment digits readable for ML Kit.
 */
object ImageUtils {

    /** Width the OCR bitmap is upscaled to before it is handed to ML Kit. */
    const val OCR_TARGET_WIDTH = 1024

    /** Longest side of the buffer handed to the seven-segment reader. */
    const val SEGMENT_TARGET_WIDTH = 360

    /** Fallback ROI when the caller has no guide frame geometry. */
    val DISPLAY_ROI = NormalizedRect(left = 0.20f, top = 0.30f, right = 0.80f, bottom = 0.70f)

    /**
     * Crops the normalised ROI out of [source] and rotates the result upright in one step.
     * Cropping first means the expensive rotation only touches the pixels we actually use
     * (the previous code rotated the full 1080p frame four times a second).
     */
    fun cropRotated(source: Bitmap, roi: NormalizedRect, rotationDegrees: Int): Bitmap {
        val upright = rotationDegrees % 360
        // The ROI is expressed in upright coordinates; map it back onto the raw frame.
        val raw = when (upright) {
            90 -> NormalizedRect(roi.top, 1f - roi.right, roi.bottom, 1f - roi.left)
            180 -> NormalizedRect(1f - roi.right, 1f - roi.bottom, 1f - roi.left, 1f - roi.top)
            270 -> NormalizedRect(1f - roi.bottom, roi.left, 1f - roi.top, roi.right)
            else -> roi
        }
        val cropped = cropNormalized(source, raw)
        if (upright == 0) return cropped
        val matrix = Matrix().apply { postRotate(upright.toFloat()) }
        return Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
    }

    fun cropNormalized(source: Bitmap, roi: NormalizedRect): Bitmap {
        val left = (source.width * roi.left).toInt().coerceIn(0, source.width - 1)
        val top = (source.height * roi.top).toInt().coerceIn(0, source.height - 1)
        val right = (source.width * roi.right).toInt().coerceIn(left + 1, source.width)
        val bottom = (source.height * roi.bottom).toInt().coerceIn(top + 1, source.height)
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    /** Downscales so the longest side is at most [maxWidth] px (keeps the aspect ratio). */
    fun downscale(source: Bitmap, maxWidth: Int): Bitmap {
        if (source.width <= maxWidth) return source
        val height = max(1, source.height * maxWidth / source.width)
        return Bitmap.createScaledBitmap(source, maxWidth, height, true)
    }

    /** Row-major luminance buffer (0..255). */
    fun toLuminance(source: Bitmap): IntArray {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val luma = IntArray(pixels.size)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            luma[i] = (r * 299 + g * 587 + b * 114) / 1000
        }
        return luma
    }

    /**
     * Turns a photographed LCD into something a Latin OCR model can actually read:
     * Otsu binarisation with polarity correction, a morphological closing that welds the
     * seven segments of a glyph into one solid stroke, a white margin and an upscale.
     *
     * The closing step is the important one — without it ML Kit sees seven disconnected
     * bars per digit instead of a character.
     */
    fun enhanceForOcr(source: Bitmap, targetWidth: Int = OCR_TARGET_WIDTH): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source

        val luma = toLuminance(source)
        var mask = ImageOps.binarize(luma, luma.size)
        val stroke = ImageOps.strokeWidth(mask, width, height)
        if (stroke >= 2) {
            mask = ImageOps.close(mask, width, height, max(1, (stroke * 0.45f).toInt()))
        }

        // Always hand ML Kit dark text on a white background, with a quiet margin.
        val margin = max(8, height / 12)
        val outWidth = width + margin * 2
        val outHeight = height + margin * 2
        val pixels = IntArray(outWidth * outHeight) { WHITE }
        for (y in 0 until height) {
            val srcRow = y * width
            val dstRow = (y + margin) * outWidth + margin
            for (x in 0 until width) {
                if (mask[srcRow + x]) pixels[dstRow + x] = BLACK
            }
        }

        val bitmap = Bitmap.createBitmap(pixels, outWidth, outHeight, Bitmap.Config.ARGB_8888)
        if (outWidth >= targetWidth) return bitmap
        val scaledHeight = max(1, outHeight * targetWidth / outWidth)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, scaledHeight, true)
    }

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()
}
