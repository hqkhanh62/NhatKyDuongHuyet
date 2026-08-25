package com.example.nhatkyduonghuyet.ml

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton

data class ScannedGlucoseResult(
    val value: Float,
    val date: String? = null,
    val time: String? = null
)

@Singleton
class GlucoseScanner @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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
                    val date = extractDate(rawText)
                    val time = extractTime(rawText)
                    onResult(ScannedGlucoseResult(value, date, time))
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    private fun extractGlucose(text: String): Float? {
        val normalizedText = text.replace(',', '.')
        val regex = Regex("""\b(\d{1,2}\.\d)\b""")
        val match = regex.find(normalizedText)
        val value = match?.value?.toFloatOrNull()
        return if (value != null && value in 2.0f..30.0f) value else null
    }

    private fun extractTime(text: String): String? {
        val timeRegex = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
        return timeRegex.find(text)?.value
    }

    private fun extractDate(text: String): String? {
        val dateRegex = Regex("""\b(\d{4}[-/]\d{1,2}[-/]\d{1,2})\b|\b(\d{1,2}[-/]\d{1,2}[-/]\d{4})\b|\b(\d{1,2}[-/]\d{1,2})\b""")
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
                        // yyyy-??-??
                        if (p2 > 12 && p3 <= 12) {
                            // yyyy-dd-mm -> yyyy-mm-dd
                            "%04d-%02d-%02d".format(p1, p3, p2)
                        } else {
                            "%04d-%02d-%02d".format(p1, p2, p3)
                        }
                    } else {
                        // ??-??-yyyy
                        if (p1 > 12 && p2 <= 12) {
                            // dd-mm-yyyy -> yyyy-mm-dd
                            "%04d-%02d-%02d".format(p3, p2, p1)
                        } else {
                            // mm-dd-yyyy or valid dd-mm-yyyy
                            "%04d-%02d-%02d".format(p3, p1, p2)
                        }
                    }
                }
                2 -> {
                    "%04d-%02d-%02d".format(currentYear, parts[1].toInt(), parts[0].toInt())
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}