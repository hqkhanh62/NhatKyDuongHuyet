package com.example.nhatkyduonghuyet.ai

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class LSTMEngine(private val context: Context) {

    private val interpreter: Interpreter?
    private val inferenceLock = Any()

    init {
        // Load defensively: if the model asset or the Flex delegate is missing,
        // fail inference gracefully (callers handle the failure) instead of
        // crashing the whole app at startup.
        interpreter = try {
            val model = loadModel(context, "lstm_model.tflite")
            val options = Interpreter.Options().apply {
                addDelegate(FlexDelegate())
            }
            Interpreter(model, options)
        } catch (_: Exception) {
            null
        }
    }

    fun predict(input: Array<Array<FloatArray>>): Float {
        require(
            input.size == 1 &&
                input[0].size == Normalizer.SEQUENCE_LENGTH &&
                input[0].all { it.size == 1 && it[0].isFinite() }
        ) { "LSTM input phải có dạng [1, ${Normalizer.SEQUENCE_LENGTH}, 1] với giá trị hữu hạn." }

        val model = interpreter ?: throw IllegalStateException("Mô hình LSTM chưa được khởi tạo.")

        return synchronized(inferenceLock) {
            val output = Array(1) { FloatArray(1) }
            model.run(input, output)
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