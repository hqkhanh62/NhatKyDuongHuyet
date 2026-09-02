package com.example.nhatkyduonghuyet.ai

import com.example.nhatkyduonghuyet.domain.GlucosePolicy
import com.example.nhatkyduonghuyet.domain.GlucoseRiskLevel

object Normalizer {
    // The ML model is trained with a fixed 2.0-25.0 mmol/L range
    // (see CALIBRATION_MIN/MAX in train_realtime_lstm_v2.py), so the
    // normalization/denormalization MUST use that same range. Values outside
    // the display range (up to MAX_GLUCOSE_MMOL) are still accepted as valid
    // input but are clamped by normalize()/the denormalize()+coerce path.
    const val MIN_GLUCOSE_MMOL = 2.0f
    const val MAX_GLUCOSE_MMOL = 30.0f
    const val MAX_NORM_MMOL = 25.0f
    const val SEQUENCE_LENGTH = 5

    fun normalize(raw: FloatArray): FloatArray {
        if (raw.isEmpty()) return raw
        return raw.map {
            ((it - MIN_GLUCOSE_MMOL) / (MAX_NORM_MMOL - MIN_GLUCOSE_MMOL)).coerceIn(0f, 1f)
        }.toFloatArray()
    }

    fun denormalize(normalizedValue: Float): Float =
        (normalizedValue * (MAX_NORM_MMOL - MIN_GLUCOSE_MMOL)) + MIN_GLUCOSE_MMOL

    fun isValidGlucose(value: Float): Boolean =
        GlucosePolicy.isValid(value)

    fun toLstmInput(raw: FloatArray): Array<Array<FloatArray>> {
        require(raw.size == SEQUENCE_LENGTH && raw.all(::isValidGlucose)) {
            "Cần đúng $SEQUENCE_LENGTH chỉ số đường huyết hợp lệ từ $MIN_GLUCOSE_MMOL đến $MAX_GLUCOSE_MMOL mmol/L."
        }
        val normalized = normalize(raw)
        return Array(1) { Array(SEQUENCE_LENGTH) { index -> floatArrayOf(normalized[index]) } }
    }
}

sealed interface PredictionOutcome<out T> {
    data class Success<T>(val value: T) : PredictionOutcome<T>
    data class Failure(val reason: String) : PredictionOutcome<Nothing>
}

data class PredictionResult(
    val current: Float,
    val next: Float,
    val trend: Float,
    val risk: String = "Normal"
)

data class MultiStepResult(
    val forecastPoints: List<Float>,
    val maxExpected: Float,
    val minExpected: Float,
    val timeStepMinutes: Int = 360 // Default to 6 hours
)

data class RealtimeForecast(
    val nextPrediction: PredictionResult,
    val future: MultiStepResult
)

object RiskDetector {
    fun detectRisk(mmolValue: Float): String {
        return when {
            mmolValue < GlucosePolicy.LOW_THRESHOLD -> GlucoseRiskLevel.LOW.label
            mmolValue > GlucosePolicy.VERY_HIGH_THRESHOLD -> GlucoseRiskLevel.VERY_HIGH.label
            mmolValue > GlucosePolicy.HIGH_THRESHOLD -> GlucoseRiskLevel.HIGH.label
            else -> GlucoseRiskLevel.NORMAL.label
        }
    }
}