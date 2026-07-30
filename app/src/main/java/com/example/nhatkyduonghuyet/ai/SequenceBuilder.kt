
package com.example.nhatkyduonghuyet.ai

object SequenceBuilder {
    fun build(data: List<Float>): FloatArray {
        return data.takeLast(5).toFloatArray()
    }
}
