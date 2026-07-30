
package com.example.nhatkyduonghuyet.ai

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LSTMEngine(context: Context) {
    private val interpreter: Interpreter
    init {
        val bytes = context.assets.open("lstm_model.tflite").readBytes()
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        interpreter = Interpreter(buffer)
    }
    fun predict(seq: FloatArray): Float {
        val input = Array(1) { Array(5) { FloatArray(1) } }
        for (i in seq.indices) input[0][i][0] = seq[i]
        val output = Array(1) { FloatArray(1) }
        interpreter.run(input, output)
        return output[0][0]
    }
}
