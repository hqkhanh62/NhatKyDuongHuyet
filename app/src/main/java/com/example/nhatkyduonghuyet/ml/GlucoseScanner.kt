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

/** Result extracted from a glucose meter display. */
data class ScannedGlucoseResult(
    val value: Float,
    val date: String? = null,
    val time: String? = null,
    val source: String = "ML_KIT"
)

@Singleton
class GlucoseScanner @Inject constructor() {

    // Initialize ML Kit only when a real camera frame is processed.
    // This keeps the pure OCR parser usable from JVM unit tests.
    private val pixelReader = PixelGlucoseReader()
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val numberRegex = Regex(
        "(?<![0-9A-Za-z])([0-9OoQqIiLl|]{1,3}(?:\\.[0-9OoQqIiLl|]{1,2})?)(?![0-9A-Za-z])"
    )

    private val dateOrTimeRegex = Regex(
        "\\b[0-9]{1,4}[/\\-][0-9]{1,2}(?:[/\\-][0-9]{1,4})?\\b|" +
            "\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b"
    )

    fun processImage(
        image: InputImage,
        onResult: (ScannedGlucoseResult?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val candidate = extractGlucoseCandidate(visionText)
                onResult(candidate?.let { scannedResult(it.value, "ML_KIT", visionText.text) })
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    /**
     * Hybrid camera-frame analysis.
     *
     * @param roi normalized ROI of the upright image that matches the on-screen
     * guide frame. When null, falls back to the default display ROI.
     */
    fun processHybrid(
        fullBitmap: Bitmap,
        rotationDegrees: Int,
        roi: ImageUtils.NormalizedRect? = null,
        onResult: (ScannedGlucoseResult?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val upright = ImageUtils.rotateBitmap(fullBitmap, rotationDegrees)
        val frameRoi = roi ?: ImageUtils.DISPLAY_ROI

        // 1. Tight crop (exactly what the green frame shows) feeds the
        //    seven-segment pixel reader, which expects a digit-centered ROI.
        val displayCrop = ImageUtils.cropNormalized(upright, frameRoi)
        val pixelResult = try {
            pixelReader.processDisplay(displayCrop)
        } catch (_: Exception) {
            null
        }

        // 2. ML Kit gets a slightly taller crop so adjacent date/time rows on
        //    the meter display remain visible, then enhanced (grayscale +
        //    contrast stretch + upscale) which dramatically improves reading
        //    of low-contrast LCD digits.
        val ocrCrop = ImageUtils.cropNormalized(
            upright,
            expandRoiVertically(frameRoi, OCR_ROI_VERTICAL_EXPANSION)
        )
        val ocrInput = try {
            ImageUtils.enhanceForOcr(ocrCrop)
        } catch (_: Exception) {
            ocrCrop
        }

        recognizer.process(InputImage.fromBitmap(ocrInput, 0))
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                val mlKitCandidate = extractGlucoseCandidate(visionText)
                onResult(combineHybrid(pixelResult, mlKitCandidate, rawText))
            }
            .addOnFailureListener { error ->
                // If ML Kit fails, we might still have a confident pixel result.
                if (pixelResult != null &&
                    pixelResult.confidence >= PIXEL_AUTHORITATIVE_CONFIDENCE
                ) {
                    onResult(ScannedGlucoseResult(pixelResult.value, source = "PIXEL"))
                } else {
                    onError(error)
                }
            }
    }

    /** Expands a ROI vertically so OCR also sees rows just outside the frame. */
    private fun expandRoiVertically(
        roi: ImageUtils.NormalizedRect,
        fraction: Float
    ): ImageUtils.NormalizedRect {
        val padding = (roi.bottom - roi.top) * fraction
        return ImageUtils.NormalizedRect(
            left = roi.left,
            top = (roi.top - padding).coerceAtLeast(0f),
            right = roi.right,
            bottom = (roi.bottom + padding).coerceAtMost(1f)
        )
    }

    private fun scannedResult(value: Float, source: String, rawText: String) =
        ScannedGlucoseResult(
            value = value,
            date = extractDate(rawText),
            time = extractTime(rawText),
            source = source
        )

    private fun combineHybrid(
        pixel: PixelDisplayReading?,
        mlKit: SpatialGlucoseCandidate?,
        rawText: String
    ): ScannedGlucoseResult? {
        if (pixel == null && mlKit == null) return null

        if (pixel != null && pixel.confidence >= PIXEL_AUTHORITATIVE_CONFIDENCE) {
            val agrees = mlKit == null || abs(pixel.value - mlKit.value) <= HYBRID_TOLERANCE
            if (agrees) {
                return scannedResult(pixel.value, "PIXEL", rawText)
            }
            // A confident pixel reading contradicted by a weak OCR line:
            // deliver nothing and keep scanning instead of guessing.
            if (mlKit != null && !mlKit.strong) return null
        }

        // Trust ML Kit when the pixel reader is absent or not confident, or
        // when the OCR line itself carries strong evidence (unit context,
        // decimal separator or a dominant text height).
        return mlKit?.let { scannedResult(it.value, "ML_KIT", rawText) }
            ?: pixel?.takeIf { it.confidence >= PIXEL_AUTHORITATIVE_CONFIDENCE }
                ?.let { scannedResult(it.value, "PIXEL", rawText) }
    }

    private data class GlucoseCandidate(
        val value: Float,
        val score: Int,
        val position: Int
    )

    private data class SpatialGlucoseCandidate(
        val value: Float,
        val score: Int,
        val position: Int,
        /** True when the OCR line carries strong evidence of the reading. */
        val strong: Boolean
    )

    /**
     * Selects the largest plausible numeric line from ML Kit's layout tree.
     * This prevents small `DAY`, `AVG`, date and time digits from winning over
     * the large central display value.
     */
    private fun extractGlucose(visionText: Text): Float? =
        extractGlucoseCandidate(visionText)?.value

    private fun extractGlucoseCandidate(visionText: Text): SpatialGlucoseCandidate? {
        val lines = visionText.textBlocks.flatMap { it.lines }
        if (lines.isEmpty()) {
            // No layout geometry available: fall back to the text-only parser.
            return extractGlucose(visionText.text)
                ?.let { SpatialGlucoseCandidate(it, 0, 0, strong = false) }
        }

        var maxLineHeight = 0
        for (line in lines) {
            val lineHeight = line.boundingBox?.height() ?: 0
            if (lineHeight > maxLineHeight) maxLineHeight = lineHeight
        }

        val candidates = lines.mapIndexedNotNull { index, line ->
            // ML Kit may split a seven-segment reading into separate
            // elements: ["5", ".", "7"]. Rebuild the line with spaces so
            // the normalizer can recover both the decimal point and digit.
            val elementText = line.elements.joinToString(" ") { it.text }
            val lineText = elementText.ifBlank { line.text }
            val value = extractGlucose(lineText) ?: return@mapIndexedNotNull null

            val context = lineText.lowercase()
            val hasUnit = context.contains("mmol") || context.contains("mg")
            val hasLabel = hasGlucoseLabel(context)

            // Date/time rows are noise unless the same row explicitly
            // identifies a glucose value or unit.
            if (dateOrTimeRegex.containsMatchIn(lineText) && !hasUnit && !hasLabel) {
                return@mapIndexedNotNull null
            }

            val boxHeight = line.boundingBox?.height() ?: 0
            var score = boxHeight.coerceAtMost(1_000)
            if (line.text.contains('.') || line.text.contains(',')) score += 180
            if (hasUnit) score += 300
            if (context.contains("day") || context.contains("avg") ||
                context.contains("date") || context.contains("time") ||
                context.contains("mem")) score -= 500

            val dominantLine = maxLineHeight > 0 && boxHeight >= maxLineHeight * 0.5f
            SpatialGlucoseCandidate(
                value = value,
                score = score,
                position = index,
                strong = hasUnit ||
                    lineText.contains('.') ||
                    lineText.contains(',') ||
                    dominantLine
            )
        }

        return candidates
            .sortedWith(compareByDescending<SpatialGlucoseCandidate> { it.score }.thenBy { it.position })
            .firstOrNull()
    }

    /**
     * Extracts a plausible glucose value from common meter output formats:
     * 6.1, 6,1, 6 1, 110 mg/dL and 110 mg/dl.
     *
     * A number greater than 30 is not converted without an explicit mg/dL
     * context. This avoids turning an OCR error such as "81" into 4.5 mmol/L.
     */
    private fun extractGlucose(text: String): Float? {
        if (text.isBlank()) return null

        val candidates = mutableListOf<GlucoseCandidate>()
        var absolutePosition = 0

        normalizeOcrText(text).lineSequence().forEach { line ->
            val lineContext = line.lowercase()
            val hasMmolUnit = lineContext.contains("mmol")
            val hasMgUnit = lineContext.contains("mg")
            val hasLabel = hasGlucoseLabel(lineContext)

            // Date/time rows are noise unless the same row explicitly identifies
            // a glucose value or unit.
            val isDateOrTimeRow = dateOrTimeRegex.containsMatchIn(line)
            if (isDateOrTimeRow && !hasLabel && !hasMmolUnit && !hasMgUnit) {
                absolutePosition += line.length + 1
                return@forEach
            }

            numberRegex.findAll(line).forEach { match ->
                val numericToken = normalizeNumericToken(match.groupValues[1])
                val rawValue = numericToken.toFloatOrNull() ?: return@forEach
                val convertedValue = when {
                    hasMgUnit -> rawValue / MG_DL_PER_MMOL
                    rawValue > 20f && !hasMmolUnit -> return@forEach
                    rawValue in MIN_GLUCOSE..MAX_GLUCOSE -> rawValue
                    else -> return@forEach
                }

                if (convertedValue !in MIN_GLUCOSE..MAX_GLUCOSE) return@forEach

                var score = 0
                if (hasMmolUnit) score += 100
                if (hasMgUnit) score += 90
                if (rawValue % 1f != 0f) score += 25
                if (convertedValue in 3f..20f) score += 10
                if (hasLabel) score += 20

                candidates += GlucoseCandidate(
                    value = convertedValue,
                    score = score,
                    position = absolutePosition + match.range.first
                )
            }
            absolutePosition += line.length + 1
        }

        return candidates
            .sortedWith(compareByDescending<GlucoseCandidate> { it.score }.thenBy { it.position })
            .firstOrNull()
            ?.value
    }

    private fun hasGlucoseLabel(lowercaseLine: String): Boolean =
        lowercaseLine.contains("glucose") ||
            lowercaseLine.contains("sugar") ||
            lowercaseLine.contains("result") ||
            lowercaseLine.contains("value")

    /**
     * Makes common OCR errors deterministic before numeric parsing.
     * Delimiter normalization is intentionally conservative: a blank is
     * treated as a decimal separator only when followed by a unit or EOL.
     */
    private fun normalizeOcrText(text: String): String {
        var normalized = text
            .replace('\u00A0', ' ')
            .replace('٫', '.')
            .replace('，', '.')
            .replace(',', '.')

        // 6 , 1 / 6 . 1 -> 6.1, including spaces around the delimiter.
        normalized = Regex("(?<=\\d)\\s*[.]\\s*(?=\\d)")
            .replace(normalized, ".")

        // Some seven-segment displays produce "6 1 mmol/L".
        normalized = Regex(
            "(?<!\\d)(\\d{1,2})\\s+(\\d)(?=\\s*(?:mmol|mg(?:/\\s*dl)?|$))",
            RegexOption.IGNORE_CASE
        ).replace(normalized, "$1.$2")

        return normalized
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

    private fun normalizeNumericToken(token: String): String = token
        .replace('O', '0', ignoreCase = true)
        .replace('Q', '0', ignoreCase = true)
        .replace('I', '1', ignoreCase = true)
        .replace('L', '1', ignoreCase = true)
        .replace('|', '1')

    private fun extractTime(text: String): String? {
        val timeRegex = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b")
        return timeRegex.find(text)?.value
    }

    private fun extractDate(text: String): String? {
        val dateRegex = Regex(
            "\\b(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})\\b|" +
                "\\b(\\d{1,2}[-/]\\d{1,2}[-/]\\d{4})\\b|" +
                "\\b(\\d{1,2}[-/]\\d{1,2})\\b"
        )
        val match = dateRegex.find(text)?.value ?: return null

        return try {
            val parts = match.split('/', '-')
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

            when (parts.size) {
                3 -> {
                    val p1 = parts[0].toInt()
                    val p2 = parts[1].toInt()
                    val p3 = parts[2].toInt()

                    if (parts[0].length == 4) {
                        if (p2 > 12 && p3 <= 12) {
                            "%04d-%02d-%02d".format(p1, p3, p2)
                        } else {
                            "%04d-%02d-%02d".format(p1, p2, p3)
                        }
                    } else if (p1 > 12 && p2 <= 12) {
                        "%04d-%02d-%02d".format(p3, p2, p1)
                    } else {
                        "%04d-%02d-%02d".format(p3, p1, p2)
                    }
                }
                2 -> {
                    "%04d-%02d-%02d".format(currentYear, parts[1].toInt(), parts[0].toInt())
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Visible to JVM tests without exposing parsing internals to production callers. */
    internal fun extractGlucoseForTesting(text: String): Float? = extractGlucose(text)

    private companion object {
        const val MG_DL_PER_MMOL = 18.0f

        /** Vertical expansion of the frame ROI for the OCR crop. */
        const val OCR_ROI_VERTICAL_EXPANSION = 0.35f
    }
}
