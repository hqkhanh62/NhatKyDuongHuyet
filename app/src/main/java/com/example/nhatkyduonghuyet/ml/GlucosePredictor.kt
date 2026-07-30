package com.example.nhatkyduonghuyet.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
            e.printStackTrace()
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

    fun predict(fasting: Float, type: Int): Float {
        val input = arrayOf(floatArrayOf(fasting, type.toFloat()))
        val output = Array(1) { FloatArray(1) }

        interpreter?.run(input, output)
        return output[0][0]
    }
}
