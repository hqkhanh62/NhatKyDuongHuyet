package com.example.nhatkyduonghuyet.ai

import javax.inject.Inject
import javax.inject.Singleton

data class MultiStepResult(
    val hourlyForecasts: List<Float>,
    val maxExpected: Float,
    val minExpected: Float
)

@Singleton
class RealtimePredictor @Inject constructor(
    private val model: LSTMEngine
) {
    private val buffer = GlucoseBuffer(size = 5)

    fun onNewGlucose(value: Float): PredictionResult? {
        buffer.add(value)

        if (!buffer.isReady()) return null

        val input = buffer.toInputArray()
        val next = model.predict(input)
        val trend = next - value

        return PredictionResult(
            current = value,
            next = next,
            trend = trend
        )
    }
    
    fun predictFuture24Hours(): MultiStepResult? {
        val results = mutableListOf<Float>()
        val tempValues = buffer.getAll().toMutableList()
        if (tempValues.size < 5) return null

        // Forecast for 4 major time steps in the next 24 hours (roughly 6h intervals)
        repeat(4) {
            val tempBuffer = GlucoseBuffer(5)
            // Use the last 5 points (including newly predicted ones) to predict the next point
            tempValues.takeLast(5).forEach { tempBuffer.add(it) }
            
            val next = model.predict(tempBuffer.toInputArray())
            results.add(next)
            tempValues.add(next)
        }

        return MultiStepResult(
            hourlyForecasts = results,
            maxExpected = results.maxOrNull() ?: 0f,
            minExpected = results.minOrNull() ?: 0f
        )
    }
}
