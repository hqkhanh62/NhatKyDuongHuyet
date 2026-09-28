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
    val centerY: Float get() = (top + bottom) / 2f
    val centerX: Float get() = (left + right) / 2f

    fun expand(factor: Float): NormalizedRect = expand(factor, factor)

    /**
     * Grows the rect by [paddingX] of its width and [paddingY] of its height,
     * split evenly on both sides. Padding is a fraction of the rect, not of the
     * image, so the behaviour is identical for a 320.dp dialog and a full-screen
     * scanner.
     */
    fun expand(paddingX: Float, paddingY: Float): NormalizedRect {
        val dx = width * paddingX / 2f
        val dy = height * paddingY / 2f
        return NormalizedRect(
            left = left - dx,
            top = top - dy,
            right = right + dx,
            bottom = bottom + dy
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

/**
 * Region searched for the small text of the top row (mm-dd on the left, HH:mm on
 * the right) and for the labels below the big digits.
 *
 * The vertical span is deliberately the *whole* frame instead of the guide box:
 * `PreviewView` and `ImageAnalysis` are two different camera streams, and on many
 * devices the YUV stream ends up 4:3 while the preview is 16:9. The ROI mapped
 * from the guide frame then covers a narrower slice of the scene than the user
 * sees, which is exactly the "chỉ quét phần trên màn hình" symptom: the display's
 * top row fell outside the analysed crop, so the value was read from whatever
 * digits happened to be inside (often the date) and the date/time was never seen
 * at all. Sweeping the full height makes the reading independent of that aspect
 * mismatch, while the horizontal span stays tied to the guide frame so the
 * background on the sides is still excluded.
 */
fun smallTextSweepRoi(displayRoi: NormalizedRect): NormalizedRect {
    val safe = displayRoi.sanitized()
    val sidePad = SMALL_TEXT_SIDE_PAD
    return NormalizedRect(
        left = safe.left - sidePad,
        top = SWEEP_TOP,
        right = safe.right + sidePad,
        bottom = SWEEP_BOTTOM
    ).sanitized()
}

/** Fallback ROI when the viewport geometry is not known yet. */
val DEFAULT_DISPLAY_ROI = NormalizedRect(0.08f, 0.10f, 0.92f, 0.90f)

/** A guide frame smaller than this is almost certainly a measurement glitch. */
private const val MIN_ROI_FRACTION = 0.16f

/**
 * Aspect ratio (width / height) of the guide frame shared by every scanner UI.
 * Keeping one ratio means the pixel reader sees identically shaped crops
 * regardless of which screen started the scan.
 *
 * 1.34 instead of the old 1.6: a meter LCD is a block of digits plus a status row
 * of small text above them, so a wide-short frame forced users to either cut the
 * top row (losing mm-dd / HH:mm) or cut the bottom of the digits (mis-read value).
 */
const val SCAN_FRAME_ASPECT_RATIO = 1.34f

/**
 * Margin added around the guide frame before the main OCR pass. Vertical margin is
 * much larger than the horizontal one because the digits are what must not be
 * clipped: cutting the bottom third of a "7" turns it into a "1" for both ML Kit
 * and the seven-segment decoder.
 */
const val OCR_ROI_PADDING_X = 0.10f
const val OCR_ROI_PADDING_Y = 0.34f

/** Kept for callers that only care about the horizontal margin. */
const val OCR_ROI_PADDING = OCR_ROI_PADDING_X

/** Extra horizontal room given to the small-text sweep, in fractions of the image. */
const val SMALL_TEXT_SIDE_PAD = 0.06f

/** Vertical limits of the small-text sweep (top and bottom of the analysed image). */
const val SWEEP_TOP = 0.02f
const val SWEEP_BOTTOM = 0.98f
