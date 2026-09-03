package com.example.nhatkyduonghuyet.ml

import kotlin.math.max

/** Axis-aligned rectangle in preview-view coordinates (px or dp). */
data class FrameRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Pure geometry shared by the scanner UI and the camera analysis pipeline.
 *
 * The on-screen green guide frame is defined in preview-view space; this object
 * converts it into the normalized ROI of the upright camera image so the OCR
 * pipeline analyzes exactly what the user sees inside the frame (WYSIWYG).
 * All functions are free of Android dependencies and JVM-unit-testable.
 */
object ScannerGeometry {

    /** Guide frame aspect ratio (width / height), close to a meter LCD display. */
    const val GUIDE_FRAME_ASPECT = 1.5f

    /** Preferred guide frame width as a fraction of the screen width. */
    const val GUIDE_FRAME_SCREEN_WIDTH_FRACTION = 0.82f

    /** Hard upper bound (dp) so tablets keep a sane frame size. */
    const val GUIDE_FRAME_MAX_WIDTH = 420f

    /** Max fraction of the preview width the frame may occupy. */
    private const val MAX_VIEW_WIDTH_FRACTION = 0.94f

    /** Max fraction of the preview height the frame may occupy. */
    private const val MAX_VIEW_HEIGHT_FRACTION = 0.60f

    /**
     * Preferred guide frame width in dp for the current screen. Using the
     * screen (not the preview) as the reference keeps the frame equally large
     * in the full-screen scanner and in the dialog scanner.
     */
    fun preferredFrameWidth(screenWidthDp: Float): Float =
        (screenWidthDp * GUIDE_FRAME_SCREEN_WIDTH_FRACTION).coerceAtMost(GUIDE_FRAME_MAX_WIDTH)

    /**
     * Computes the centered guide frame rectangle inside a preview of the
     * given size, clamped so it always fits inside the preview.
     */
    fun computeGuideFrame(
        viewWidth: Float,
        viewHeight: Float,
        preferredWidth: Float
    ): FrameRect? {
        if (viewWidth <= 0f || viewHeight <= 0f || preferredWidth <= 0f) return null

        val maxWidth = viewWidth * MAX_VIEW_WIDTH_FRACTION
        var width = preferredWidth.coerceAtMost(maxWidth)
        var height = width / GUIDE_FRAME_ASPECT
        val maxHeight = viewHeight * MAX_VIEW_HEIGHT_FRACTION
        if (height > maxHeight) {
            height = maxHeight
            width = (height * GUIDE_FRAME_ASPECT).coerceAtMost(maxWidth)
        }

        val left = (viewWidth - width) / 2f
        val top = (viewHeight - height) / 2f
        return FrameRect(left, top, left + width, top + height)
    }

    /**
     * Maps a preview-view rectangle to the normalized ROI of the upright
     * camera image, replicating PreviewView's FILL_CENTER scale type: the
     * image is scaled so it covers the whole view and the excess is cropped.
     *
     * The mapping is scale invariant, so view units may be px or dp as long
     * as they are consistent.
     */
    fun frameToImageRoi(
        frame: FrameRect,
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): ImageUtils.NormalizedRect? {
        if (viewWidth <= 0f || viewHeight <= 0f) return null
        if (imageWidth <= 0f || imageHeight <= 0f) return null
        if (frame.width <= 0f || frame.height <= 0f) return null

        val scale = max(viewWidth / imageWidth, viewHeight / imageHeight)
        val offsetX = (viewWidth - imageWidth * scale) / 2f
        val offsetY = (viewHeight - imageHeight * scale) / 2f

        fun toImageX(x: Float) = ((x - offsetX) / scale / imageWidth).coerceIn(0f, 1f)
        fun toImageY(y: Float) = ((y - offsetY) / scale / imageHeight).coerceIn(0f, 1f)

        val left = toImageX(frame.left)
        val right = toImageX(frame.right)
        val top = toImageY(frame.top)
        val bottom = toImageY(frame.bottom)
        if (right <= left || bottom <= top) return null

        return ImageUtils.NormalizedRect(left, top, right, bottom)
    }
}
