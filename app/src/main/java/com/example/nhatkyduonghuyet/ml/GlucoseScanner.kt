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

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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
     * Extracts a plausible mmol/L value from common meter output formats:
     * 6.1, 6,1, 110 mg/dL and 110 (converted to 6.1 mmol/L when no unit is read).
     */
    private fun extractGlucose(text: String): Float? {
        if (text.isBlank()) return null

        val normalizedText = Regex("""(\d)\s*[.,]\s*(\d)""")
            .replace(
                text
                    .replace('O', '0')
                    .replace('o', '0')
                    .replace('I', '1')
                    .replace('l', '1')
                    .replace(',', '.')
            ) { match -> "${match.groupValues[1]}.${match.groupValues[2]}" }

        val numberRegex = Regex("(?<![0-9])([0-9]{1,3}(?:\\.[0-9]{1,2})?)(?![0-9])")
        val candidates = numberRegex.findAll(normalizedText).mapNotNull { match ->
            val rawValue = match.groupValues[1].toFloatOrNull() ?: return@mapNotNull null
            val start = match.range.first
            val end = match.range.last + 1

            // Do not treat a date, clock value or a number embedded in a unit as glucose.
            val before = normalizedText.substring(maxOf(0, start - 3), start)
            val after = normalizedText.substring(end, minOf(normalizedText.length, end + 5))
            if (before.contains(':') || after.startsWith(':') || before.endsWith('/') ||
                after.startsWith('/') || before.endsWith('-') || after.startsWith('-')) {
                return@mapNotNull null
            }

            val contextStart = maxOf(0, start - 24)
            val contextEnd = minOf(normalizedText.length, end + 24)
            val context = normalizedText.substring(contextStart, contextEnd).lowercase()
            val hasMmolUnit = context.contains("mmol") || context.contains("mmol/l")
            val hasMgUnit = context.contains("mg") || context.contains("mg/dl")

            val convertedValue = when {
                hasMgUnit -> rawValue / MG_DL_PER_MMOL
                rawValue > MAX_MMOL_WITHOUT_UNIT -> rawValue / MG_DL_PER_MMOL
                else -> rawValue
            }

            if (convertedValue !in MIN_GLUCOSE..MAX_GLUCOSE) {
                return@mapNotNull null
            }

            var score = 0
            if (hasMmolUnit) score += 100
            if (hasMgUnit || rawValue > MAX_MMOL_WITHOUT_UNIT) score += 80
            if (rawValue % 1f != 0f) score += 20
            if (convertedValue in 3f..20f) score += 10

            GlucoseCandidate(convertedValue, score, start)
        }.toList()

        return candidates
            .sortedWith(compareByDescending<GlucoseCandidate> { it.score }.thenBy { it.position })
            .firstOrNull()
            ?.value
    }

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

    private companion object {
        const val MG_DL_PER_MMOL = 18.0f
        const val MIN_GLUCOSE = 2.0f
        const val MAX_GLUCOSE = 30.0f
        const val MAX_MMOL_WITHOUT_UNIT = 30.0f
    }
}
