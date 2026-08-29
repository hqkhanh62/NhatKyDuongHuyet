package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

object ImageUtils {

    /**
     * ROI for the main glucose value on the On Call Plus display.
     * These normalized coordinates should be calibrated based on the green frame in the UI.
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
}
