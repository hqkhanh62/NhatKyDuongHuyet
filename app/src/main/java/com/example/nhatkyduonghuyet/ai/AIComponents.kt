package com.example.nhatkyduonghuyet.ai

object Normalizer {
    // These should match the values used in train_realtime_lstm_v2.py
    private const val SCALER_MIN = 2.0f
    private const val SCALER_MAX = 25.0f

    fun normalize(raw: FloatArray): FloatArray {
        if (raw.isEmpty()) return raw
        return raw.map { ((it - SCALER_MIN) / (SCALER_MAX - SCALER_MIN)).coerceIn(0f, 1f) }.toFloatArray()
    }

    fun denormalize(normalizedValue: Float): Float {
        return (normalizedValue * (SCALER_MAX - SCALER_MIN)) + SCALER_MIN
    }
}

data class PredictionResult(
    val current: Float,
    val next: Float,
    val trend: Float,
    val risk: String = "Normal"
)

data class MultiStepResult(
    val hourlyForecasts: List<Float>,
    val maxExpected: Float,
    val minExpected: Float
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
