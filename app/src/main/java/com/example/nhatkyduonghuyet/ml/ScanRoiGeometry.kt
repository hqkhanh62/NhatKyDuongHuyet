package com.example.nhatkyduonghuyet.ml

/**
 * Normalized rectangle (0f..1f) inside an image or a view.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun expand(factor: Float): NormalizedRect {
        val dx = width * factor / 2f
        val dy = height * factor / 2f
        return NormalizedRect(
            left = (left - dx).coerceIn(0f, 1f),
            top = (top - dy).coerceIn(0f, 1f),
            right = (right + dx).coerceIn(0f, 1f),
            bottom = (bottom + dy).coerceIn(0f, 1f)
        ).sanitized()
    }

    fun sanitized(): NormalizedRect {
        val l = left.coerceIn(0f, 1f)
        val t = top.coerceIn(0f, 1f)
        val r = right.coerceIn(0f, 1f)
        val b = bottom.coerceIn(0f, 1f)
        return NormalizedRect(
            left = minOf(l, r),
            top = minOf(t, b),
            right = maxOf(l, r),
            bottom = maxOf(t, b)
        )
    }
}

/**
 * Maps the green guide frame drawn on top of a `PreviewView` onto the region of
 * the analysis frame that actually contains those pixels.
 *
 * This is the core of the scanning accuracy problem: `PreviewView.ScaleType.FILL_CENTER`
 * center-crops the camera buffer to fill the view, so a fixed normalized ROI
 * (for example 0.20..0.80) points at a *different* part of the meter depending on
 * how large the preview surface is. A 320.dp dialog preview and a full-screen
 * preview therefore analysed two different crops of the same scene, which is why
 * the dialog scanner in DayDetail mis-read values far more often.
 *
 * With this mapping both screens analyse exactly what the user framed in green.
 *
 * All sizes are in the same (upright) orientation: the analysis bitmap must
 * already be rotated so that it matches what is displayed.
 */
fun scanRoiForViewport(
    imageWidth: Int,
    imageHeight: Int,
    viewWidth: Int,
    viewHeight: Int,
    frameWidth: Float,
    frameHeight: Float,
    fillCenter: Boolean = true
): NormalizedRect {
    if (imageWidth <= 0 || imageHeight <= 0 ||
        viewWidth <= 0 || viewHeight <= 0 ||
        frameWidth <= 0f || frameHeight <= 0f
    ) {
        return DEFAULT_DISPLAY_ROI
    }

    // Fraction of the view occupied by the guide frame, centered.
    val frameW = (frameWidth / viewWidth).coerceAtMost(1f)
    val frameH = (frameHeight / viewHeight).coerceAtMost(1f)

    // How much of the camera buffer is visible in the view.
    val viewAspect = viewWidth.toFloat() / viewHeight
    val imageAspect = imageWidth.toFloat() / imageHeight

    // FILL_CENTER: the buffer is scaled up until it covers the view, so part of
    // it is cropped away. FIT_CENTER: the whole buffer is visible (letterboxed).
    var visibleW = 1f
    var visibleH = 1f
    if (fillCenter) {
        if (imageAspect > viewAspect) {
            // Buffer is wider than the view -> left/right cropped.
            visibleW = viewAspect / imageAspect
        } else {
            // Buffer is taller than the view -> top/bottom cropped.
            visibleH = imageAspect / viewAspect
        }
    } else {
        if (imageAspect > viewAspect) {
            visibleH = viewAspect / imageAspect
        } else {
            visibleW = imageAspect / viewAspect
        }
    }

    val roiW = (frameW * visibleW).coerceIn(MIN_ROI_FRACTION, 1f)
    val roiH = (frameH * visibleH).coerceIn(MIN_ROI_FRACTION, 1f)

    return NormalizedRect(
        left = 0.5f - roiW / 2f,
        top = 0.5f - roiH / 2f,
        right = 0.5f + roiW / 2f,
        bottom = 0.5f + roiH / 2f
    ).sanitized()
}

/** Fallback ROI when the viewport geometry is not known yet. */
val DEFAULT_DISPLAY_ROI = NormalizedRect(0.15f, 0.28f, 0.85f, 0.72f)

/** A guide frame smaller than this is almost certainly a measurement glitch. */
private const val MIN_ROI_FRACTION = 0.1f

/**
 * Aspect ratio (width / height) of the guide frame shared by every scanner UI.
 * Keeping one ratio means the pixel reader sees identically shaped crops
 * regardless of which screen started the scan.
 */
const val SCAN_FRAME_ASPECT_RATIO = 1.6f

/** Extra margin added around the guide frame before OCR, as a fraction. */
const val OCR_ROI_PADDING = 0.12f
