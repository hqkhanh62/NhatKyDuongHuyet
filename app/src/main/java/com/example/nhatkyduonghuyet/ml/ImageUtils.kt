package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint

object ImageUtils {

    /**
     * Fallback ROI used when the caller does not know the on-screen guide frame.
     * Prefer passing the measured ROI from [scanRoiForViewport].
     */
    val DISPLAY_ROI: NormalizedRect = DEFAULT_DISPLAY_ROI

    /** Minimum width (px) fed to OCR; small crops are upscaled to this. */
    private const val MIN_OCR_WIDTH = 720

    /** Never blow a crop up more than this, it only adds blur. */
    private const val MAX_UPSCALE = 4f

    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return source
        val matrix = Matrix()
        matrix.postRotate(normalized.toFloat())
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun cropNormalized(source: Bitmap, roi: NormalizedRect): Bitmap {
        val safe = roi.sanitized()
        val left = (source.width * safe.left).toInt().coerceIn(0, source.width - 1)
        val top = (source.height * safe.top).toInt().coerceIn(0, source.height - 1)
        val right = (source.width * safe.right).toInt().coerceIn(left + 1, source.width)
        val bottom = (source.height * safe.bottom).toInt().coerceIn(top + 1, source.height)

        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    /**
     * Upscales a small crop so ML Kit sees enough pixels per digit. ML Kit's
     * latin recognizer needs roughly 32 px of glyph height; a crop taken from a
     * small preview can fall well below that, which is a common cause of
     * mis-read seven-segment digits.
     */
    fun upscaleForOcr(source: Bitmap, minWidth: Int = MIN_OCR_WIDTH): Bitmap {
        if (source.width >= minWidth) return source
        val scale = (minWidth.toFloat() / source.width).coerceAtMost(MAX_UPSCALE)
        val targetWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Grayscale + contrast boost. LCD meter displays are low contrast under
     * indoor lighting; normalizing them makes both the pixel reader and ML Kit
     * far more stable.
     */
    fun enhanceForOcr(source: Bitmap, contrast: Float = 1.6f): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val saturation = ColorMatrix().apply { setSaturation(0f) }
        val translate = -(contrast - 1f) * 128f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        saturation.postConcat(contrastMatrix)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(saturation)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /** Crop + enhance + upscale pipeline shared by every scanner entry point. */
    fun prepareOcrBitmap(rotated: Bitmap, roi: NormalizedRect): Bitmap =
        upscaleForOcr(enhanceForOcr(cropNormalized(rotated, roi)))
}
