package com.example.nhatkyduonghuyet.ai

object Normalizer {
    // These should ideally match the values printed by train_lstm_model.py
    // Currently using reasonable defaults for blood glucose (mmol/L)
    private const val SCALER_MIN = 4.1f
    private const val SCALER_MAX = 13.8f

    fun normalize(raw: FloatArray): FloatArray {
        if (raw.isEmpty()) return raw
        return raw.map { ((it - SCALER_MIN) / (SCALER_MAX - SCALER_MIN)).coerceIn(0f, 1f) }.toFloatArray()
    }

    fun denormalize(normalizedValue: Float): Float {
        return (normalizedValue * (SCALER_MAX - SCALER_MIN)) + SCALER_MIN
    }
}

data class PredictionResult(
    val value: Float, // Predicted value in mmol/L
    val risk: String
)

object RiskDetector {
    fun detectRisk(mmolValue: Float): String {
        return when {
            mmolValue < 4.0f -> "⚠ Low Sugar"
            mmolValue > 10.0f -> "⚠ High Sugar"
            else -> "✅ Normal"
        }
    }
}
