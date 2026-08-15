package com.example.nhatkyduonghuyet.ai

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class LSTMEngine(private val context: Context) {

    private val interpreter: Interpreter
    private val inferenceLock = Any()

    init {
        val model = loadModel(context, "lstm_model.tflite")
        val options = Interpreter.Options().apply {
            addDelegate(FlexDelegate())
        }
        interpreter = Interpreter(model, options)
    }

    fun predict(input: Array<Array<FloatArray>>): Float {
        require(
            input.size == 1 &&
                input[0].size == Normalizer.SEQUENCE_LENGTH &&
                input[0].all { it.size == 1 && it[0].isFinite() }
        ) { "LSTM input phải có dạng [1, ${Normalizer.SEQUENCE_LENGTH}, 1] với giá trị hữu hạn." }

        return synchronized(inferenceLock) {
            val output = Array(1) { FloatArray(1) }
            interpreter.run(input, output)
            check(output[0][0].isFinite()) { "Mô hình LSTM trả về giá trị không hợp lệ." }
            Normalizer.denormalize(output[0][0])
        }
    }

    private fun loadModel(context: Context, name: String): MappedByteBuffer =
        context.assets.openFd(name).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        }
}