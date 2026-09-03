package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerGeometryTest {

    @Test
    fun `preferred frame width is a screen fraction capped at 420dp`() {
        assertEquals(360f * 0.82f, ScannerGeometry.preferredFrameWidth(360f), 0.001f)
        assertEquals(420f, ScannerGeometry.preferredFrameWidth(600f), 0.001f)
    }

    @Test
    fun `frame is centered and keeps preferred size when view is large enough`() {
        val frame = ScannerGeometry.computeGuideFrame(1000f, 2000f, 500f)
        assertNotNull(frame)
        frame!!
        assertEquals(500f, frame.width, 0.001f)
        assertEquals(500f / ScannerGeometry.GUIDE_FRAME_ASPECT, frame.height, 0.001f)
        assertEquals(250f, frame.left, 0.001f)
        assertEquals(750f, frame.right, 0.001f)
        assertEquals((2000f - frame.height) / 2f, frame.top, 0.001f)
    }

    @Test
    fun `frame width is clamped to the view width`() {
        val frame = ScannerGeometry.computeGuideFrame(400f, 1200f, 500f)
        assertNotNull(frame)
        assertEquals(400f * 0.94f, frame!!.width, 0.001f)
    }

    @Test
    fun `frame height is clamped to the view height`() {
        val frame = ScannerGeometry.computeGuideFrame(1000f, 300f, 900f)
        assertNotNull(frame)
        assertEquals(300f * 0.60f, frame!!.height, 0.001f)
        assertTrue(frame.width <= 1000f * 0.94f)
    }

    @Test
    fun `degenerate view or preference returns null`() {
        assertNull(ScannerGeometry.computeGuideFrame(0f, 100f, 500f))
        assertNull(ScannerGeometry.computeGuideFrame(100f, 100f, 0f))
    }

    @Test
    fun `roi maps exactly when view and image share the aspect ratio`() {
        // View 1000x2000, image 500x1000: FILL_CENTER scale 2, no cropping.
        val frame = ScannerGeometry.computeGuideFrame(1000f, 2000f, 500f)!!
        val roi = ScannerGeometry.frameToImageRoi(frame, 1000f, 2000f, 500f, 1000f)!!
        assertEquals(frame.left / 1000f, roi.left, 0.001f)
        assertEquals(frame.right / 1000f, roi.right, 0.001f)
        assertEquals(frame.top / 2000f, roi.top, 0.001f)
        assertEquals(frame.bottom / 2000f, roi.bottom, 0.001f)
    }

    @Test
    fun `roi accounts for FILL_CENTER vertical cropping`() {
        // Square view, tall image: the image is cropped vertically to fill.
        val frame = ScannerGeometry.computeGuideFrame(1000f, 1000f, 500f)!!
        val roi = ScannerGeometry.frameToImageRoi(frame, 1000f, 1000f, 500f, 1000f)!!
        // scale = 2 -> displayed 1000x2000, offsetY = -500.
        // top = (333.33 + 500) / 2 / 1000 = 0.41667
        assertEquals(0.41667f, roi.top, 0.001f)
        assertEquals(0.58333f, roi.bottom, 0.001f)
        assertEquals(frame.left / 1000f, roi.left, 0.001f)
        assertEquals(frame.right / 1000f, roi.right, 0.001f)
    }

    @Test
    fun `roi stays inside image bounds`() {
        val frame = ScannerGeometry.computeGuideFrame(1000f, 1000f, 900f)!!
        val roi = ScannerGeometry.frameToImageRoi(frame, 1000f, 1000f, 500f, 1000f)!!
        assertTrue(roi.left >= 0f && roi.right <= 1f)
        assertTrue(roi.top >= 0f && roi.bottom <= 1f)
        assertTrue(roi.right > roi.left && roi.bottom > roi.top)
    }

    @Test
    fun `roi is scale invariant between px and dp coordinates`() {
        val framePx = ScannerGeometry.computeGuideFrame(1000f, 2000f, 500f)!!
        val roiPx = ScannerGeometry.frameToImageRoi(framePx, 1000f, 2000f, 720f, 1280f)!!

        val frameDp = ScannerGeometry.computeGuideFrame(500f, 1000f, 250f)!!
        val roiDp = ScannerGeometry.frameToImageRoi(frameDp, 500f, 1000f, 720f, 1280f)!!

        assertEquals(roiPx.left, roiDp.left, 0.001f)
        assertEquals(roiPx.top, roiDp.top, 0.001f)
        assertEquals(roiPx.right, roiDp.right, 0.001f)
        assertEquals(roiPx.bottom, roiDp.bottom, 0.001f)
    }

    @Test
    fun `degenerate mapping inputs return null`() {
        val frame = ScannerGeometry.computeGuideFrame(1000f, 2000f, 500f)!!
        assertNull(ScannerGeometry.frameToImageRoi(frame, 1000f, 2000f, 0f, 1280f))
        assertNull(ScannerGeometry.frameToImageRoi(frame, 0f, 0f, 720f, 1280f))
    }
}
