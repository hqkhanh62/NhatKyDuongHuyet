package com.example.nhatkyduonghuyet.ai

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LSTMEngine @Inject constructor(private val context: Context) {

    private val interpreter: Interpreter

    init {
        val model = loadModel(context, "lstm_model.tflite")
        interpreter = Interpreter(model)
    }

    fun predict(input: Array<Array<FloatArray>>): Float {
        val output = Array(1) { FloatArray(1) }
        interpreter.run(input, output)
        
        // De-normalize result (0-1 -> mmol/L)
        // Using the calibrated Scaler Min/Max
        val min = 4.1f
        val max = 13.8f
        return (output[0][0] * (max - min)) + min
    }

    private fun loadModel(context: Context, name: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(name)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }
}
