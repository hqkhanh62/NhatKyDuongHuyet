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
    private const val MIN_OCR_WIDTH = 900

    /**
     * Minimum height (px) fed to OCR. Without this the upscale only looked at the
     * width, so a wide-but-short crop of a meter display was never enlarged and
     * the small status row (mm-dd / HH:mm) stayed at ~12 px of glyph height - far
     * below what ML Kit can read reliably.
     */
    private const val MIN_OCR_HEIGHT = 420

    /** Never blow the main crop up more than this, it only adds blur. */
    private const val MAX_UPSCALE = 4f

    /** The small-text strip is tiny on purpose, so it may be blown up harder. */
    private const val SMALL_TEXT_MIN_WIDTH = 1400
    private const val SMALL_TEXT_MIN_HEIGHT = 220
    private const val SMALL_TEXT_MAX_UPSCALE = 8f

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
     * Upscales a crop so ML Kit sees enough pixels per glyph, honouring both axes.
     *
     * The latin recognizer needs roughly 32 px of glyph height. Sizing only the
     * width (the old behaviour) left short crops untouched: a full-width frame of
     * a meter display is 900+ px wide but its date row can be 12 px tall, and that
     * row is exactly what the time/date layers need.
     */
    fun upscaleForOcr(source: Bitmap, minWidth: Int = MIN_OCR_WIDTH): Bitmap =
        scaleToCover(source, minWidth, MIN_OCR_HEIGHT, MAX_UPSCALE)

    /** Scale so that width >= [minWidth] *and* height >= [minHeight], capped at [maxScale]. */
    fun scaleToCover(source: Bitmap, minWidth: Int, minHeight: Int, maxScale: Float): Bitmap {
        if (source.width <= 0 || source.height <= 0) return source
        if (source.width >= minWidth && source.height >= minHeight) return source
        val widthScale = minWidth.toFloat() / source.width
        val heightScale = minHeight.toFloat() / source.height
        val scale = maxOf(widthScale, heightScale).coerceAtMost(maxScale)
        if (scale <= 1.001f) return source
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

    /**
     * Prepares the full-height sweep image that is searched for the small status
     * row. Harder contrast than the main pass: the mm-dd / HH:mm glyphs are thin
     * and sit next to the segment labels, and at this size a soft edge is enough
     * for ML Kit to merge two digits into one.
     */
    fun prepareSmallTextBitmap(rotated: Bitmap, roi: NormalizedRect): Bitmap {
        val cropped = cropNormalized(rotated, roi)
        val enhanced = enhanceForOcr(cropped, contrast = SMALL_TEXT_CONTRAST)
        return scaleToCover(
            source = enhanced,
            minWidth = SMALL_TEXT_MIN_WIDTH,
            minHeight = SMALL_TEXT_MIN_HEIGHT,
            maxScale = SMALL_TEXT_MAX_UPSCALE
        )
    }

    private const val SMALL_TEXT_CONTRAST = 2.1f
}
