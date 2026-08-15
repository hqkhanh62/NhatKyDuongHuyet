package com.example.nhatkyduonghuyet.ml

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton

data class ScannedGlucoseResult(
    val value: Float,
    val date: String? = null, // yyyy-MM-dd format
    val time: String? = null  // HH:mm format
)

class GlucoseScanner {

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
        // Regex for HH:mm or HH:mm AM/PM
        val timeRegex = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
        return timeRegex.find(text)?.value
    }

    private fun extractDate(text: String): String? {
        // Look for YYYY-MM-DD, DD/MM/YYYY, or DD/MM
        val dateRegex = Regex("""\b(\d{4}[-/]\d{1,2}[-/]\d{1,2})\b|\b(\d{1,2}[-/]\d{1,2}[-/]\d{4})\b|\b(\d{1,2}[-/]\d{1,2})\b""")
        val match = dateRegex.find(text)?.value ?: return null
        
        return try {
            val parts = match.split('/', '-')
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            
            when (parts.size) {
                3 -> {
                    if (parts[0].length == 4) {
                        // YYYY-MM-DD
                        "%04d-%02d-%02d".format(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                    } else {
                        // DD/MM/YYYY
                        "%04d-%02d-%02d".format(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    }
                }
                2 -> {
                    // DD/MM
                    "%04d-%02d-%02d".format(currentYear, parts[1].toInt(), parts[0].toInt())
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
