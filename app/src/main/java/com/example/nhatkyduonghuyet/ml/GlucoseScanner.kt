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

@Singleton
class GlucoseScanner @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun processImage(
        image: InputImage,
        onSuccess: (ScannedGlucoseResult) -> Unit,
        onNoResult: () -> Unit,
        onError: (Exception) -> Unit,
        onComplete: () -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                val value = extractGlucose(rawText)
                if (value != null) {
                    val date = extractDate(rawText)
                    val time = extractTime(rawText)
                    onSuccess(ScannedGlucoseResult(value, date, time))
                } else {
                    onNoResult()
                }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
            .addOnCompleteListener {
                // The CameraX frame is released only after ML Kit has finished reading it.
                onComplete()
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
        // Look for DD/MM or MM/DD or YYYY-MM-DD
        val dateRegex = Regex("""\b(\d{4}[-/]\d{1,2}[-/]\d{1,2})\b|\b(\d{1,2}[-/]\d{1,2})\b""")
        val match = dateRegex.find(text)?.value ?: return null
        
        // Normalize to yyyy-MM-dd if possible, otherwise keep raw for further processing
        return if (match.length <= 5) {
            // Likely DD/MM, prepend current year
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val parts = match.split('/', '-')
            if (parts.size == 2) {
                // Assumption: DD/MM
                "%04d-%02d-%02d".format(currentYear, parts[1].toIntOrNull() ?: 1, parts[0].toIntOrNull() ?: 1)
            } else match
        } else {
            match.replace('/', '-')
        }
    }
}
