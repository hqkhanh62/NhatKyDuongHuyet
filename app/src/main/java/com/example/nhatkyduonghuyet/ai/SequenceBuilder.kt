package com.example.nhatkyduonghuyet.ai

import java.util.ArrayDeque

class GlucoseBuffer(private val size: Int = 5) {

    private val buffer = ArrayDeque<Float>()

    fun add(value: Float) {
        if (buffer.size >= size) {
            buffer.removeFirst()
        }
        buffer.addLast(value)
    }

    fun isReady(): Boolean = buffer.size == size

    fun toInputArray(): Array<Array<FloatArray>> {
        val arr = Array(1) { Array(size) { FloatArray(1) } }
        
        val min = 4.1f
        val max = 13.8f
        
        buffer.forEachIndexed { i, v ->
            val normalized = ((v - min) / (max - min)).coerceIn(0f, 1f)
            arr[0][i][0] = normalized
        }

        return arr
    }

    fun getAll(): List<Float> = buffer.toList()
    
    fun clear() {
        buffer.clear()
    }
}
