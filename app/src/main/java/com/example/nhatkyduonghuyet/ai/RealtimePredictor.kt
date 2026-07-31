package com.example.nhatkyduonghuyet.ai

import javax.inject.Inject
import javax.inject.Singleton

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
    
    fun predictFuture(steps: Int): List<Float> {
        val results = mutableListOf<Float>()
        val tempValues = buffer.getAll().toMutableList()
        if (tempValues.size < 5) return emptyList()

        repeat(steps) {
            val tempBuffer = GlucoseBuffer(5)
            tempValues.takeLast(5).forEach { tempBuffer.add(it) }
            
            val next = model.predict(tempBuffer.toInputArray())
            results.add(next)
            tempValues.add(next)
        }

        return results
    }
}
