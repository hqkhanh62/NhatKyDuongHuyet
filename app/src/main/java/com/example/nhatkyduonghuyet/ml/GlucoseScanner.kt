package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** Where a delivered reading came from. */
enum class ScanSource { SEVEN_SEGMENT, ML_KIT, CONSENSUS }

/** Why a frame produced no reading — used to give the user actionable feedback. */
enum class ScanRejection { BLURRED, GLARE, LOW_CONTRAST, NOT_FOUND, DISAGREEMENT }

/** Result extracted from a glucose meter display. */
data class ScannedGlucoseResult(
    val value: Float,
    val date: String? = null,
    val time: String? = null,
    val source: ScanSource = ScanSource.ML_KIT
)

/** Outcome of analysing one camera frame. */
sealed interface ScanOutcome {
    data class Reading(val result: ScannedGlucoseResult) : ScanOutcome
    data class Status(val status: MeterStatus) : ScanOutcome
    data class Rejected(val reason: ScanRejection) : ScanOutcome
}

/**
 * Reads a glucose value from a camera frame.
 *
 * Two independent readers run on every frame:
 *  * [SevenSegmentReader] decodes the LCD directly from pixels;
 *  * ML Kit reads the pre-processed crop as text (units, date and time come from here).
 *
 * A value is only delivered when the evidence is strong: either both readers agree, or the
 * single reader that produced a value did so with strong evidence of its own. Everything
 * else is reported as a rejection so the UI can tell the user what to fix.
 */
@Singleton
class GlucoseScanner @Inject constructor() {

    private val segmentReader = SevenSegmentReader()

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Analyses one frame.
     *
     * @param roi normalised ROI of the upright image matching the on-screen guide frame.
     */
    fun processFrame(
        frame: Bitmap,
        rotationDegrees: Int,
        roi: NormalizedRect? = null,
        onOutcome: (ScanOutcome) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val frameRoi = roi ?: ImageUtils.DISPLAY_ROI

        // 1. Exactly what the green frame shows, slightly inset so the meter bezel does not
        //    bleed into the crop, feeds the seven-segment reader.
        val displayCrop = try {
            ImageUtils.downscale(
                ImageUtils.cropRotated(frame, frameRoi.inset(SEGMENT_ROI_INSET), rotationDegrees),
                ImageUtils.SEGMENT_TARGET_WIDTH
            )
        } catch (error: Exception) {
            onError(error)
            return
        }

        val luma = ImageUtils.toLuminance(displayCrop)
        val quality = FrameQuality.inspect(luma, displayCrop.width, displayCrop.height)
        if (!quality.isUsable) {
            onOutcome(
                ScanOutcome.Rejected(
                    when {
                        quality.hasGlare -> ScanRejection.GLARE
                        quality.isBlurred -> ScanRejection.BLURRED
                        else -> ScanRejection.LOW_CONTRAST
                    }
                )
            )
            return
        }

        val segmented = try {
            segmentReader.read(luma, displayCrop.width, displayCrop.height)
                ?.takeIf { it.value in MIN_GLUCOSE..MAX_GLUCOSE }
        } catch (_: Exception) {
            null
        }

        // 2. ML Kit gets a taller crop so the unit and the date/time rows stay visible.
        val ocrInput = try {
            ImageUtils.enhanceForOcr(
                ImageUtils.cropRotated(
                    frame,
                    frameRoi.expandVertically(OCR_ROI_VERTICAL_EXPANSION),
                    rotationDegrees
                )
            )
        } catch (error: Exception) {
            onError(error)
            return
        }

        recognizer.process(InputImage.fromBitmap(ocrInput, 0))
            .addOnSuccessListener { visionText ->
                onOutcome(combine(segmented, visionText))
            }
            .addOnFailureListener { error ->
                // ML Kit failed: only a very confident seven-segment reading may stand alone.
                if (segmented != null && segmented.margin >= SOLO_MARGIN) {
                    onOutcome(
                        ScanOutcome.Reading(
                            ScannedGlucoseResult(segmented.value, source = ScanSource.SEVEN_SEGMENT)
                        )
                    )
                } else {
                    onError(error)
                }
            }
    }

    internal fun combine(segmented: SegmentedReading?, visionText: Text): ScanOutcome {
        val rawText = visionText.text
        val ocr = bestTextEvidence(visionText)

        if (ocr == null && segmented == null) {
            GlucoseTextParser.status(rawText)?.let { return ScanOutcome.Status(it) }
            return ScanOutcome.Rejected(ScanRejection.NOT_FOUND)
        }

        // Two independent readers agreeing is the strongest evidence available.
        if (segmented != null && ocr != null) {
            return if (abs(segmented.value - ocr.value) <= AGREEMENT_TOLERANCE) {
                reading(segmented.value, ScanSource.CONSENSUS, rawText)
            } else {
                ScanOutcome.Rejected(ScanRejection.DISAGREEMENT)
            }
        }

        if (segmented != null) {
            return if (segmented.margin >= SOLO_MARGIN) {
                reading(segmented.value, ScanSource.SEVEN_SEGMENT, rawText)
            } else {
                ScanOutcome.Rejected(ScanRejection.NOT_FOUND)
            }
        }

        val evidence = ocr!!
        return if (evidence.isStrong) {
            reading(evidence.value, ScanSource.ML_KIT, rawText)
        } else {
            ScanOutcome.Rejected(ScanRejection.NOT_FOUND)
        }
    }

    private fun reading(value: Float, source: ScanSource, rawText: String) =
        ScanOutcome.Reading(
            ScannedGlucoseResult(
                value = value,
                date = extractDate(rawText),
                time = extractTime(rawText),
                source = source
            )
        )

    /**
     * Picks the most plausible line of the OCR result. Layout matters: the measurement is the
     * largest text on a meter display, while `DAY`, `AVG`, date and time are printed small.
     */
    private fun bestTextEvidence(visionText: Text): GlucoseTextEvidence? {
        val lines = visionText.textBlocks.flatMap { it.lines }
        if (lines.isEmpty()) return GlucoseTextParser.parse(visionText.text)

        val tallest = lines.maxOfOrNull { it.boundingBox?.height() ?: 0 } ?: 0
        var best: GlucoseTextEvidence? = null
        var bestScore = Int.MIN_VALUE
        lines.forEach { line ->
            // ML Kit can split a seven-segment reading into ["5", ".", "7"].
            val text = line.elements.joinToString(" ") { it.text }.ifBlank { line.text }
            val evidence = GlucoseTextParser.parseLine(text) ?: return@forEach
            val height = line.boundingBox?.height() ?: 0
            val dominant = tallest > 0 && height >= tallest * DOMINANT_LINE_RATIO
            var score = GlucoseTextParser.score(evidence)
            if (dominant) score += 60
            if (score > bestScore) {
                bestScore = score
                best = evidence
            }
        }
        return best
    }

    private fun extractTime(text: String): String? =
        Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""").find(text)?.value

    private fun extractDate(text: String): String? {
        val match = Regex(
            """\b(\d{4}[-/]\d{1,2}[-/]\d{1,2})\b|\b(\d{1,2}[-/]\d{1,2}[-/]\d{4})\b"""
        ).find(text)?.value ?: return null

        return try {
            val parts = match.split('/', '-')
            if (parts.size != 3) return null
            val first = parts[0].toInt()
            val second = parts[1].toInt()
            val third = parts[2].toInt()
            if (parts[0].length == 4) {
                if (second > 12 && third <= 12) {
                    "%04d-%02d-%02d".format(first, third, second)
                } else {
                    "%04d-%02d-%02d".format(first, second, third)
                }
            } else if (first > 12 && second <= 12) {
                "%04d-%02d-%02d".format(third, second, first)
            } else {
                "%04d-%02d-%02d".format(third, first, second)
            }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        /** Vertical expansion of the frame ROI for the OCR crop. */
        const val OCR_ROI_VERTICAL_EXPANSION = 0.35f

        /** Inset applied to the seven-segment crop to keep the meter bezel out. */
        const val SEGMENT_ROI_INSET = 0.04f

        /** Two readers count as agreeing within this many mmol/L. */
        const val AGREEMENT_TOLERANCE = 0.05f

        /** Segment margin required when the seven-segment reader has no corroboration. */
        const val SOLO_MARGIN = 1.0f

        /** Fraction of the tallest line above which a line counts as the main display. */
        const val DOMINANT_LINE_RATIO = 0.55f
    }
}
