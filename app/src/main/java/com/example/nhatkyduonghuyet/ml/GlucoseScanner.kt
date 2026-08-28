package com.example.nhatkyduonghuyet.ml

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton

/** Result extracted from a glucose meter display. */
data class ScannedGlucoseResult(
    val value: Float,
    val date: String? = null,
    val time: String? = null
)

@Singleton
class GlucoseScanner @Inject constructor() {

    // Initialize ML Kit only when a real camera frame is processed.
    // This keeps the pure OCR parser usable from JVM unit tests.
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun processImage(
        image: InputImage,
        onResult: (ScannedGlucoseResult?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                val value = extractGlucose(rawText)
                if (value != null) {
                    onResult(
                        ScannedGlucoseResult(
                            value = value,
                            date = extractDate(rawText),
                            time = extractTime(rawText)
                        )
                    )
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    private data class GlucoseCandidate(
        val value: Float,
        val score: Int,
        val position: Int
    )

    /**
     * Extracts a plausible glucose value from common meter output formats:
     * 6.1, 6,1, 6 1, 110 mg/dL and 110 mg/dl.
     *
     * A number greater than 30 is not converted without an explicit mg/dL
     * context. This avoids turning an OCR error such as "81" into 4.5 mmol/L.
     */
    private fun extractGlucose(text: String): Float? {
        if (text.isBlank()) return null

        val numberRegex = Regex(
            "(?<![0-9A-Za-z])([0-9OoQqIiLl|]{1,3}(?:\\.[0-9OoQqIiLl|]{1,2})?)(?![0-9A-Za-z])"
        )
        val dateOrTimeRegex = Regex(
            "\\b[0-9]{1,4}[/\\-][0-9]{1,2}(?:[/\\-][0-9]{1,4})?\\b|" +
                "\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b"
        )
        val candidates = mutableListOf<GlucoseCandidate>()
        var absolutePosition = 0

        normalizeOcrText(text).lineSequence().forEach { line ->
            val lineContext = line.lowercase()
            val hasMmolUnit = lineContext.contains("mmol")
            val hasMgUnit = lineContext.contains("mg")
            val hasGlucoseLabel = lineContext.contains("glucose") ||
                lineContext.contains("sugar") ||
                lineContext.contains("result") ||
                lineContext.contains("value")

            // Date/time rows are noise unless the same row explicitly identifies
            // a glucose value or unit.
            val isDateOrTimeRow = dateOrTimeRegex.containsMatchIn(line)
            if (isDateOrTimeRow && !hasGlucoseLabel && !hasMmolUnit && !hasMgUnit) {
                absolutePosition += line.length + 1
                return@forEach
            }

            numberRegex.findAll(line).forEach { match ->
                val numericToken = normalizeNumericToken(match.groupValues[1])
                val rawValue = numericToken.toFloatOrNull() ?: return@forEach
                val convertedValue = when {
                    hasMgUnit -> rawValue / MG_DL_PER_MMOL
                    rawValue in MIN_GLUCOSE..MAX_GLUCOSE -> rawValue
                    else -> return@forEach
                }

                if (convertedValue !in MIN_GLUCOSE..MAX_GLUCOSE) return@forEach

                var score = 0
                if (hasMmolUnit) score += 100
                if (hasMgUnit) score += 90
                if (rawValue % 1f != 0f) score += 25
                if (convertedValue in 3f..20f) score += 10
                if (hasGlucoseLabel) score += 20

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
        const val MIN_GLUCOSE = 2.0f
        const val MAX_GLUCOSE = 30.0f
    }
}
