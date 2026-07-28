package com.example.nhatkyduonghuyet.ai

object Normalizer {
    fun normalize(raw: FloatArray): FloatArray {
        if (raw.isEmpty()) return raw
        // Simple MinMax scaling simulation
        val max = 20f // Max mmol/L
        return raw.map { it / max }.toFloatArray()
    }
}

data class PredictionResult(
    val value: Float, // Internal value (can be mg/dL or mmol/L depending on logic)
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
