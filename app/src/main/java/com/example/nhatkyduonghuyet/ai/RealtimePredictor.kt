package com.example.nhatkyduonghuyet.ai

import com.example.nhatkyduonghuyet.ai.RiskDetector
import javax.inject.Inject
import javax.inject.Singleton

class RealtimePredictor(
    private val model: LSTMEngine
) {
    private val buffer = GlucoseBuffer(size = 5)

    fun onNewGlucose(value: Float): PredictionResult? {
        buffer.add(value)

        if (!buffer.isReady()) return null

        val input = buffer.toInputArray()
        val next = model.predict(input)
        val trend = next - value
        val risk = RiskDetector.detectRisk(next)

        return PredictionResult(
            current = value,
            next = next,
            trend = trend,
            risk = risk
        )
    }
    
    /**
     * Recursive forecasting for the next 24 hours (4 steps of 6 hours)
     */
    fun predictFuture24Hours(): MultiStepResult? {
        val results = mutableListOf<Float>()
        val tempValues = buffer.getAll().toMutableList()
        if (tempValues.size < 5) return null

        repeat(4) {
            val tempBuffer = GlucoseBuffer(size = 5)
            // Feed the last 5 values (original or predicted) back into the LSTM
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
