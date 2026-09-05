package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class FrameQualityTest {

    private val width = 200
    private val height = 140

    private fun display(ink: Int = 20, paper: Int = 245): IntArray =
        IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            // A few vertical bars standing in for seven-segment strokes.
            if (y in 30..110 && (x / 12) % 2 == 0 && x in 30..170) ink else paper
        }

    private fun blur(source: IntArray, radius: Int): IntArray {
        val out = IntArray(source.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                for (dy in -radius..radius) {
                    val ny = y + dy
                    if (ny !in 0 until height) continue
                    for (dx in -radius..radius) {
                        val nx = x + dx
                        if (nx !in 0 until width) continue
                        sum += source[ny * width + nx]
                        count++
                    }
                }
                out[y * width + x] = sum / count
            }
        }
        return out
    }

    @Test
    fun `a sharp display is usable`() {
        assertTrue(FrameQuality.inspect(display(), width, height).isUsable)
    }

    @Test
    fun `an inverted display is usable`() {
        assertTrue(FrameQuality.inspect(display(ink = 240, paper = 18), width, height).isUsable)
    }

    @Test
    fun `a hopelessly blurred frame is rejected`() {
        val report = FrameQuality.inspect(blur(display(), 15), width, height)
        assertTrue("focus=${report.focus}", report.isBlurred)
        assertFalse(report.isUsable)
    }

    @Test
    fun `moderately blurred frames are still accepted`() {
        // The seven-segment reader copes with camera softness; only hopeless frames are
        // dropped, otherwise the scanner would refuse to read a normal handheld shot.
        for (radius in intArrayOf(1, 3, 5)) {
            assertFalse(
                "radius $radius",
                FrameQuality.inspect(blur(display(), radius), width, height).isBlurred
            )
        }
    }

    @Test
    fun `a frame washed out by a reflection is rejected`() {
        val base = display(ink = 110, paper = 205)
        val glared = IntArray(base.size) { index ->
            val x = (index % width - width * 0.4) / (width * 0.3)
            val y = (index / width - height * 0.4) / (height * 0.35)
            (base[index] + 120 * exp(-(x * x + y * y))).toInt().coerceIn(0, 255)
        }
        val report = FrameQuality.inspect(glared, width, height)
        assertTrue("clipped=${report.clippedRatio}", report.hasGlare)
        assertFalse(report.isUsable)
    }

    @Test
    fun `an empty frame is rejected`() {
        val report = FrameQuality.inspect(IntArray(width * height) { 200 }, width, height)
        assertTrue(report.isFlat)
        assertFalse(report.isUsable)
    }
}
