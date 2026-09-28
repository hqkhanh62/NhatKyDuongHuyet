package com.example.nhatkyduonghuyet.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The decoder is validated against synthetic renders of seven-segment meter
 * displays: plain readings, labels, glare, ghost digits, backlit (dark)
 * displays, dropped decimal points, handheld shear, sensor noise and blur.
 *
 * The same suite was used to design the algorithm (see
 * tools/prototype_seven_segment_decoder.py) so regressions are caught here
 * without needing a device.
 */
class SevenSegmentDecoderTest {

    // ---------------------------------------------------------------------
    // Minimal seven-segment renderer (canonical segment geometry).
    // ---------------------------------------------------------------------
    private object Renderer {

        private val segments = mapOf(
            'A' to floatArrayOf(0.08f, 0.000f, 0.92f, 0.110f),
            'B' to floatArrayOf(0.890f, 0.130f, 1.000f, 0.445f),
            'C' to floatArrayOf(0.890f, 0.555f, 1.000f, 0.870f),
            'D' to floatArrayOf(0.08f, 0.890f, 0.92f, 1.000f),
            'E' to floatArrayOf(0.000f, 0.555f, 0.110f, 0.870f),
            'F' to floatArrayOf(0.000f, 0.130f, 0.110f, 0.445f),
            'G' to floatArrayOf(0.08f, 0.445f, 0.92f, 0.555f)
        )

        private val patterns = mapOf(
            '0' to "ABCDEF",
            '1' to "BC",
            '2' to "ABGED",
            '3' to "ABCDG",
            '4' to "FGBC",
            '5' to "AFGCD",
            '6' to "AFGEDC",
            '7' to "ABC",
            '8' to "ABCDEFG",
            '9' to "ABCDFG"
        )

        fun argb(gray: Int): Int =
            (0xFF shl 24) or (gray.coerceIn(0, 255) shl 16) or
                (gray.coerceIn(0, 255) shl 8) or gray.coerceIn(0, 255)

        fun render(
            text: String,
            width: Int = 320,
            height: Int = 150,
            digitHeight: Int = 84,
            background: Int = 200,
            foreground: Int = 60,
            ghostDigit: Char? = null,
            label: Boolean = true,
            glare: Boolean = false,
            top: Int = 12,
            slotGap: Int = 6,
            shear: Float = 0f,
            noise: Int = 0,
            blur: Int = 0
        ): IntArray {
            val px = IntArray(width * height) { argb(background) }

            fun rect(x0: Float, y0: Float, x1: Float, y1: Float, gray: Int) {
                val color = argb(gray)
                val ys = maxOf(0, y0.roundToInt()) until minOf(height, y1.roundToInt())
                val xs = maxOf(0, x0.roundToInt()) until minOf(width, x1.roundToInt())
                for (y in ys) {
                    for (x in xs) {
                        val shifted = if (shear != 0f) {
                            (x + shear * (y - top - digitHeight / 2f)).roundToInt()
                        } else x
                        if (shifted in 0 until width) px[y * width + shifted] = color
                    }
                }
            }

            val slotWidth = (0.56f * digitHeight).toInt()
            var x = 18f
            for (ch in text) {
                if (ch == '.') {
                    x += 0.10f * digitHeight
                    val size = (0.15f * digitHeight).toInt()
                    rect(x, (top + digitHeight - size).toFloat(), x + size, (top + digitHeight).toFloat(), foreground)
                    x += size + 0.10f * digitHeight
                    continue
                }
                val color = if (ch == ghostDigit) 178 else foreground
                for (seg in patterns.getValue(ch)) {
                    val r = segments.getValue(seg)
                    rect(
                        x + r[0] * slotWidth, top + r[1] * digitHeight,
                        x + r[2] * slotWidth, top + r[3] * digitHeight,
                        color
                    )
                }
                x += slotWidth + slotGap
            }

            if (label) {
                val labelY = top + digitHeight + 14
                for (i in 0 until 14) {
                    rect(30f + i * 11f, labelY.toFloat(), 36f + i * 11f, (labelY + 16).toFloat(), 150)
                }
            }
            if (glare) {
                rect(width * 0.62f, 0f, width * 0.95f, height * 0.35f, 255)
            }

            var out = px
            repeat(blur) {
                val next = out.copyOf()
                for (y in 1 until height - 1) {
                    for (x2 in 1 until width - 1) {
                        var acc = 0
                        for (dy in -1..1) {
                            for (dx in -1..1) {
                                acc += (out[(y + dy) * width + x2 + dx] shr 16) and 0xFF
                            }
                        }
                        next[y * width + x2] = argb(acc / 9)
                    }
                }
                out = next
            }
            if (noise > 0) {
                val random = Random(noise)
                for (i in out.indices) {
                    if (random.nextFloat() < 0.25f) {
                        val d = random.nextInt(-noise, noise + 1)
                        out[i] = argb(((out[i] shr 16) and 0xFF) + d)
                    }
                }
            }
            return out
        }
    }

    private fun assertReads(
        expected: Float,
        text: String,
        label: Boolean = true,
        glare: Boolean = false,
        ghostDigit: Char? = null,
        shear: Float = 0f,
        noise: Int = 0,
        blur: Int = 0,
        background: Int = 200,
        foreground: Int = 60
    ) {
        val pixels = Renderer.render(
            text, label = label, glare = glare, ghostDigit = ghostDigit,
            shear = shear, noise = noise, blur = blur,
            background = background, foreground = foreground
        )
        val decoding = SevenSegmentDecoder.decode(pixels, 320, 150)
        assertEquals("display \"$text\"", expected, decoding?.value ?: Float.NaN, 0.051f)
    }

    private fun assertRefused(
        text: String,
        label: Boolean = false,
        glare: Boolean = false,
        ghostDigit: Char? = null,
        background: Int = 200,
        foreground: Int = 60
    ) {
        val pixels = Renderer.render(
            text, label = label, glare = glare, ghostDigit = ghostDigit,
            background = background, foreground = foreground
        )
        assertNull("display \"$text\" must be refused", SevenSegmentDecoder.decode(pixels, 320, 150))
    }

    // ---------------------------------------------------------------------
    // Plain readings
    // ---------------------------------------------------------------------

    @Test
    fun `reads plain two digit decimal readings`() {
        assertReads(5.7f, "5.7")
        assertReads(6.1f, "6.1")
        assertReads(4.2f, "4.2")
        assertReads(9.9f, "9.9")
        assertReads(7.7f, "7.7")
        assertReads(2.5f, "2.5")
        assertReads(3.1f, "3.1")
    }

    @Test
    fun `reads readings with a leading one`() {
        assertReads(10.1f, "10.1")
        assertReads(11.4f, "11.4")
        assertReads(15.2f, "15.2")
        assertReads(17.3f, "17.3")
        assertReads(12.8f, "12.8", label = false)
    }

    @Test
    fun `reads two integer digit readings`() {
        assertReads(25.0f, "25.0")
    }

    @Test
    fun `reads without a label row`() {
        assertReads(7.8f, "7.8", label = false)
    }

    @Test
    fun `reads wide digit spacing`() {
        val pixels = Renderer.render("12.8", label = false, slotGap = 20)
        val decoding = SevenSegmentDecoder.decode(pixels, 320, 150)
        assertEquals(12.8f, decoding?.value ?: Float.NaN, 0.051f)
    }

    // ---------------------------------------------------------------------
    // Real-world stress
    // ---------------------------------------------------------------------

    @Test
    fun `ignores a glare patch`() {
        assertReads(6.4f, "6.4", glare = true)
    }

    @Test
    fun `ignores a faint ghost digit`() {
        // Leading unlit "0" (LCD ghosting) is below the threshold: 05.7 -> 5.7.
        assertReads(5.7f, "05.7", ghostDigit = '0', label = false)
        // A faint ghost 8 is invisible to the decoder, so the reading is 5.7.
        assertReads(5.7f, "85.7", ghostDigit = '8', label = false)
    }

    @Test
    fun `refuses a clearly visible ghost digit instead of misreading`() {
        // Ghost pushed into the dark class: reading becomes 85.7 -> refused.
        val faint = Renderer.render("85.7", ghostDigit = '8', label = false)
        val dark = IntArray(faint.size) { i ->
            val gray = (faint[i] shr 16) and 0xFF
            Renderer.argb(if (gray == 178) 100 else gray)
        }
        assertNull(SevenSegmentDecoder.decode(dark, 320, 150))
    }

    @Test
    fun `reads a backlit display with dark background`() {
        assertReads(8.3f, "8.3", background = 55, foreground = 215, label = true)
    }

    @Test
    fun `recovers a dropped decimal point`() {
        // The decimal blob was not detected; the meter always shows one
        // decimal digit in mmol mode, so 57 -> 5.7 and 101 -> 10.1.
        assertReads(5.7f, "57", label = false)
        assertReads(10.1f, "101", label = false)
        assertReads(15.2f, "152", label = false)
    }

    @Test
    fun `keeps plausible two digit integers`() {
        assertReads(25.0f, "25", label = false)
    }

    @Test
    fun `tolerates handheld shear`() {
        assertReads(5.7f, "5.7", shear = 0.07f)
        assertReads(10.1f, "10.1", shear = 0.07f)
        assertReads(8.3f, "8.3", shear = 0.07f)
    }

    @Test
    fun `tolerates sensor noise`() {
        assertReads(6.9f, "6.9", noise = 18)
    }

    @Test
    fun `tolerates mild blur`() {
        assertReads(7.2f, "7.2", blur = 1)
    }

    @Test
    fun `blur noise and shear never produce a wrong value`() {
        val pixels = Renderer.render("9.4", blur = 1, noise = 10, shear = 0.04f)
        val decoding = SevenSegmentDecoder.decode(pixels, 320, 150)
        assertTrue(decoding == null || kotlin.math.abs(decoding.value - 9.4f) < 0.051f)
    }

    // ---------------------------------------------------------------------
    // Refusals (a wrong "null" is always better than a wrong number)
    // ---------------------------------------------------------------------

    @Test
    fun `refuses a single digit`() {
        assertRefused("7")
    }

    @Test
    fun `refuses an empty display`() {
        assertRefused("")
    }

    @Test
    fun `refuses a flat frame`() {
        val pixels = IntArray(320 * 150) { Renderer.argb(128) }
        assertNull(SevenSegmentDecoder.decode(pixels, 320, 150))
    }

    @Test
    fun `refuses a tiny crop`() {
        val pixels = IntArray(100) { Renderer.argb(200) }
        assertNull(SevenSegmentDecoder.decode(pixels, 10, 10))
    }

    @Test
    fun `refuses out of range values`() {
        assertRefused("35.7")
    }
}
