package com.example.nhatkyduonghuyet.ai

object Normalizer {
    fun normalize(raw: FloatArray): FloatArray {
        if (raw.isEmpty()) return raw
        // In production with a scaler.save, you would use pre-computed mean/std or min/max
        // Since scaler.save is binary, we use a simple normalization here.
        // If scaler.save is a MinMax scaler, we'd need the min/max values.
        val max = raw.maxOrNull() ?: 1f
        return raw.map { it / max }.toFloatArray()
    }
}

data class PredictionResult(
    val value: Float,
    val risk: String
)

object RiskDetector {
    fun detectRisk(value: Float): String {
        return when {
            value < 70 -> "⚠ Low Sugar"
            value > 180 -> "⚠ High Sugar"
            else -> "✅ Normal"
        }
    }
}
