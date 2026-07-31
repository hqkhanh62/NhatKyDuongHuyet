package com.example.nhatkyduonghuyet.ml

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlucoseScanner @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun processImage(
        image: InputImage,
        onSuccess: (Float) -> Unit,
        onError: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val glucose = extractGlucose(visionText.text)
                if (glucose != null) {
                    onSuccess(glucose)
                }
            }
            .addOnFailureListener { e ->
                onError(it)
            }
    }

    private fun extractGlucose(text: String): Float? {
        // Regex for typical mmol/L format like 5.6 or 12.3
        // Also handling comma as decimal separator if OCR misreads
        val normalizedText = text.replace(',', '.')
        val regex = Regex("""\b(\d{1,2}\.\d)\b""")
        val match = regex.find(normalizedText)
        
        val value = match?.value?.toFloatOrNull()
        
        // Validation: Health clean (Typical range 2.0 - 30.0 mmol/L)
        return if (value != null && value in 2.0f..30.0f) value else null
    }
}
