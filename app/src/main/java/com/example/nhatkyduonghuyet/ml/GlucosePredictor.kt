package com.example.nhatkyduonghuyet.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlucosePredictor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null

    init {
        try {
            val file = loadModelFile(context, "glucose_model.tflite")
            interpreter = Interpreter(file)
        } catch (e: Exception) {
            android.util.Log.e("GlucosePredictor", "Model load failed", e)
        }
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    suspend fun predict(fasting: Float, type: Int): Float = withContext(Dispatchers.Default) {
        if (interpreter == null) {
            return@withContext -1f // Return sentinel for error
        }

        try {
            val input = arrayOf(floatArrayOf(fasting, type.toFloat()))
            val output = Array(1) { FloatArray(1) }
            interpreter?.run(input, output)
            output[0][0]
        } catch (e: Exception) {
            android.util.Log.e("GlucosePredictor", "Prediction failed", e)
            -1f
        }
    }
}
