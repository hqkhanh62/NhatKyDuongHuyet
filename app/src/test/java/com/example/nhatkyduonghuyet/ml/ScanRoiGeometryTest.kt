package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanRoiGeometryTest {

    /**
     * The core regression: a small dialog preview and a full-screen preview must
     * analyse the same physical region of the meter, otherwise the same green
     * frame means two different crops.
     */
    @Test
    fun `same frame proportion yields same roi regardless of preview size`() {
        val small = scanRoiForViewport(
            imageWidth = 1080, imageHeight = 1920,
            viewWidth = 900, viewHeight = 1200,
            frameWidth = 900 * 0.8f, frameHeight = 1200 * 0.3f
        )
        val large = scanRoiForViewport(
            imageWidth = 1080, imageHeight = 1920,
            viewWidth = 1800, viewHeight = 2400,
            frameWidth = 1800 * 0.8f, frameHeight = 2400 * 0.3f
        )
        assertEquals(small.left, large.left, 1e-4f)
        assertEquals(small.top, large.top, 1e-4f)
        assertEquals(small.right, large.right, 1e-4f)
        assertEquals(small.bottom, large.bottom, 1e-4f)
    }

    @Test
    fun `roi is centered`() {
        val roi = scanRoiForViewport(1920, 1080, 1080, 1920, 800f, 500f)
        assertEquals(0.5f, (roi.left + roi.right) / 2f, 1e-4f)
        assertEquals(0.5f, (roi.top + roi.bottom) / 2f, 1e-4f)
    }

    @Test
    fun `fill center cropping shrinks the mapped roi on the cropped axis`() {
        // 16:9 buffer shown in a 9:16 view: left/right of the buffer is cropped,
        // so a frame spanning the full view width covers less of the buffer.
        val roi = scanRoiForViewport(
            imageWidth = 1920, imageHeight = 1080,
            viewWidth = 1080, viewHeight = 1920,
            frameWidth = 1080f, frameHeight = 675f
        )
        assertTrue(roi.width < 0.5f)
        assertTrue(roi.height > 0.3f)
    }

    @Test
    fun `invalid geometry falls back to the default roi`() {
        assertEquals(DEFAULT_DISPLAY_ROI, scanRoiForViewport(0, 0, 0, 0, 0f, 0f))
    }

    @Test
    fun `roi stays inside the frame bounds`() {
        val roi = scanRoiForViewport(1080, 1920, 1080, 1920, 5000f, 5000f)
        assertTrue(roi.left >= 0f && roi.top >= 0f)
        assertTrue(roi.right <= 1f && roi.bottom <= 1f)
    }

    @Test
    fun `expand grows the rect symmetrically and clamps`() {
        val roi = NormalizedRect(0.4f, 0.4f, 0.6f, 0.6f).expand(0.5f)
        assertEquals(0.35f, roi.left, 1e-4f)
        assertEquals(0.65f, roi.right, 1e-4f)

        val clamped = NormalizedRect(0f, 0f, 1f, 1f).expand(0.5f)
        assertEquals(0f, clamped.left, 1e-4f)
        assertEquals(1f, clamped.right, 1e-4f)
    }
}
