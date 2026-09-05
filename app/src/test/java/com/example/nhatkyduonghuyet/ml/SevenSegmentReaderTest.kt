package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Renders synthetic seven-segment displays so the reader can be tested on the JVM.
 * The old pixel reader needed a device to run at all, which is exactly why its
 * thresholding bug survived.
 */
private object Display {

    private val PATTERNS = mapOf(
        '0' to "ABCDEF", '1' to "BC", '2' to "ABGED", '3' to "ABCDG", '4' to "FGBC",
        '5' to "AFGCD", '6' to "AFGECD", '7' to "ABC", '8' to "ABCDEFG", '9' to "ABCDFG"
    )

    // left, top, right, bottom inside a digit cell
    private val DRAW = mapOf(
        'A' to floatArrayOf(0.18f, 0.05f, 0.82f, 0.16f),
        'B' to floatArrayOf(0.84f, 0.11f, 0.97f, 0.48f),
        'C' to floatArrayOf(0.84f, 0.52f, 0.97f, 0.89f),
        'D' to floatArrayOf(0.18f, 0.84f, 0.82f, 0.95f),
        'E' to floatArrayOf(0.03f, 0.52f, 0.16f, 0.89f),
        'F' to floatArrayOf(0.03f, 0.11f, 0.16f, 0.48f),
        'G' to floatArrayOf(0.18f, 0.45f, 0.82f, 0.56f)
    )

    fun render(
        text: String,
        width: Int = 420,
        height: Int = 280,
        blockWidth: Float = 0.85f,
        blockHeight: Float = 0.66f,
        centreX: Float = 0.5f,
        centreY: Float = 0.5f,
        ink: Int = 20,
        paper: Int = 245,
        shear: Float = 0f
    ): IntArray {
        val canvas = IntArray(width * height) { paper }
        val digits = text.filter { it.isDigit() }
        val count = digits.length
        val blockW = width * blockWidth
        val blockH = height * blockHeight
        val x0 = width * centreX - blockW / 2f
        val y0 = height * centreY - blockH / 2f
        val cell = blockW / count

        fun fill(l: Float, t: Float, r: Float, b: Float) {
            val yStart = t.toInt().coerceIn(0, height - 1)
            val yEnd = b.toInt().coerceIn(0, height)
            val xStart = l.toInt().coerceIn(0, width - 1)
            val xEnd = r.toInt().coerceIn(0, width)
            for (y in yStart until yEnd) {
                val slant = (shear * ((y0 + blockH / 2f) - y)).roundToInt()
                for (x in xStart until xEnd) {
                    val nx = x + slant
                    if (nx in 0 until width) canvas[y * width + nx] = ink
                }
            }
        }

        digits.forEachIndexed { index, ch ->
            val cellX = x0 + index * cell
            val glyphW = cell * 0.85f
            for (segment in PATTERNS.getValue(ch)) {
                val d = DRAW.getValue(segment)
                fill(
                    cellX + d[0] * glyphW,
                    y0 + d[1] * blockH,
                    cellX + d[2] * glyphW,
                    y0 + d[3] * blockH
                )
            }
        }

        val dot = text.indexOf('.')
        if (dot > 0) {
            val before = text.take(dot).count { it.isDigit() }
            val dotX = x0 + before * cell - cell * 0.13f
            val size = blockH * 0.09f
            fill(dotX, y0 + blockH * 0.86f, dotX + size, y0 + blockH * 0.86f + size)
        }
        return canvas
    }

    fun blur(source: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val out = IntArray(source.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var n = 0
                for (dy in -radius..radius) {
                    val ny = y + dy
                    if (ny !in 0 until height) continue
                    for (dx in -radius..radius) {
                        val nx = x + dx
                        if (nx !in 0 until width) continue
                        sum += source[ny * width + nx]
                        n++
                    }
                }
                out[y * width + x] = sum / n
            }
        }
        return out
    }
}

class SevenSegmentReaderTest {

    private val reader = SevenSegmentReader()
    private val width = 420
    private val height = 280

    private fun read(
        text: String,
        blockWidth: Float = 0.85f,
        blockHeight: Float = 0.66f,
        ink: Int = 20,
        paper: Int = 245,
        shear: Float = 0f,
        blurRadius: Int = 0,
        centreX: Float = 0.5f,
        centreY: Float = 0.5f
    ): SegmentedReading? {
        var pixels = Display.render(
            text, width, height, blockWidth, blockHeight, centreX, centreY, ink, paper, shear
        )
        if (blurRadius > 0) pixels = Display.blur(pixels, width, height, blurRadius)
        return reader.read(pixels, width, height)
    }

    private fun assertReads(expected: String) {
        val result = read(expected)
        assertNotNull("no reading for $expected", result)
        assertEquals(expected, result!!.text)
        assertTrue("margin too low for $expected", result.margin >= 0.55f)
    }

    @Test
    fun `reads every digit of a two digit decimal display`() {
        listOf("0.0", "1.1", "2.2", "3.3", "4.4", "5.5", "6.6", "7.7", "8.8", "9.9")
            .forEach { assertReads(it) }
    }

    @Test
    fun `reads typical glucose values`() {
        listOf("5.7", "6.1", "4.0", "9.4", "12.3", "10.0", "19.9", "15.6", "2.9")
            .forEach { assertReads(it) }
    }

    @Test
    fun `reads the whole plausible mmol range`() {
        var value = 30
        var checked = 0
        while (value <= 199) {
            val text = "${value / 10}.${value % 10}"
            val result = read(text)
            assertNotNull("no reading for $text", result)
            assertEquals(text, result!!.text)
            checked++
            value += 7
        }
        assertTrue(checked > 20)
    }

    @Test
    fun `reads inverted display`() {
        val result = read("5.7", ink = 240, paper = 18)
        assertEquals("5.7", result?.text)
    }

    @Test
    fun `reads low contrast photographed lcd`() {
        val result = read("12.3", ink = 110, paper = 205, blurRadius = 2)
        assertEquals("12.3", result?.text)
    }

    @Test
    fun `reads slanted italic digits`() {
        assertEquals("5.7", read("5.7", shear = 0.18f, blurRadius = 1)?.text)
        assertEquals("12.3", read("12.3", shear = 0.15f, blurRadius = 1)?.text)
    }

    @Test
    fun `reads blurred handheld frame`() {
        assertEquals("6.1", read("6.1", blurRadius = 3)?.text)
    }

    @Test
    fun `reads a display that fills only part of the guide frame`() {
        assertEquals("5.7", read("5.7", blockWidth = 0.6f, blockHeight = 0.5f)?.text)
    }

    @Test
    fun `rejects a blank frame`() {
        assertNull(reader.read(IntArray(width * height) { 240 }, width, height))
    }

    @Test
    fun `rejects random noise`() {
        val random = java.util.Random(42)
        val noise = IntArray(width * height) { random.nextInt(256) }
        assertNull(reader.read(noise, width, height))
    }

    @Test
    fun `rejects a display clipped by the guide frame`() {
        // Digits wider than the ROI: the reading would be missing a leading digit.
        assertNull(read("8.8", blockWidth = 1.15f))
        assertNull(read("8.8", blockWidth = 1.3f))
    }

    @Test
    fun `rejects an integer display when a decimal point is required`() {
        // A mmol/L meter always shows one decimal; "57" must not be read as 5.7 or 57.
        val pixels = Display.render("57", width, height)
        assertNull(reader.read(pixels, width, height))
    }

    @Test
    fun `rejects glyphs too small inside the guide frame`() {
        assertNull(read("5.7", blockWidth = 0.25f, blockHeight = 0.2f))
    }

    @Test
    fun `never returns a wrong value across the sweep`() {
        var wrong = 0
        var delivered = 0
        var value = 25
        while (value <= 299) {
            val text = "${value / 10}.${value % 10}"
            for (blur in intArrayOf(0, 2, 3)) {
                for (shear in floatArrayOf(0f, 0.12f)) {
                    val result = read(text, blurRadius = blur, shear = shear)
                    if (result != null) {
                        delivered++
                        if (result.text != text) wrong++
                    }
                }
            }
            value += 11
        }
        assertTrue("nothing was read at all", delivered > 50)
        assertEquals("wrong readings delivered", 0, wrong)
    }

    @Test
    fun `otsu separates a bimodal histogram`() {
        val pixels = IntArray(1000) { if (it < 400) 30 else 220 }
        val threshold = ImageOps.otsu(pixels)
        assertTrue("threshold $threshold", threshold in 30..219)
    }

    @Test
    fun `binarisation picks the digits on an inverted display`() {
        val pixels = IntArray(1000) { if (it < 200) 240 else 20 }
        val mask = ImageOps.binarize(pixels)
        // The minority class (the glyphs) must be the ink, whatever the polarity.
        assertEquals(0.2f, ImageOps.inkRatio(mask), 0.01f)
    }

    @Test
    fun `margin drops when segments are ambiguous`() {
        val clean = read("8.8")!!
        val smudged = read("8.8", ink = 150, paper = 190)
        assertTrue(clean.margin >= 0.55f)
        if (smudged != null) assertTrue(smudged.margin <= clean.margin + 0.001f)
    }

    @Test
    fun `reading exposes digit count`() {
        assertEquals(2, read("5.7")?.digitCount)
        assertEquals(3, read("12.3")?.digitCount)
    }

    @Test
    fun `blurred and sharp frames agree`() {
        val sharp = read("7.7")
        val blurred = read("7.7", blurRadius = 2)
        assertNotNull(sharp)
        assertNotNull(blurred)
        assertTrue(abs(sharp!!.value - blurred!!.value) < 0.001f)
    }
}
