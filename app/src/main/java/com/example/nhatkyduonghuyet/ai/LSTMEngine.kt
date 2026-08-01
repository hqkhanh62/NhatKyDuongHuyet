package com.example.nhatkyduonghuyet.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LSTMEngine @Inject constructor(@ApplicationContext private val context: Context) {

    private val interpreter: Interpreter

    init {
        val model = loadModel(context, "lstm_model.tflite")
        // LSTM models with Flex ops require the FlexDelegate
        val options = Interpreter.Options().apply {
            addDelegate(FlexDelegate())
        }
        interpreter = Interpreter(model, options)
    }

    fun predict(input: Array<Array<FloatArray>>): Float {
        val output = Array(1) { FloatArray(1) }
        interpreter.run(input, output)
        
        // Calibration from training: MIN=4.1, MAX=13.8
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
