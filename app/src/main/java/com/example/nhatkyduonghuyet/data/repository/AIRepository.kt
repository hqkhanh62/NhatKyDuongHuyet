package com.example.nhatkyduonghuyet.data.repository

import android.content.Context
import com.example.nhatkyduonghuyet.ai.*
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LogEntryDao
) {
    private var interpreter: Interpreter? = null

    init {
        try {
            interpreter = Interpreter(loadModelFile())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("lstm_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    suspend fun runPrediction(raw: FloatArray): PredictionResult = withContext(Dispatchers.Default) {
        val normalized = Normalizer.normalize(raw)
        
        // Prepare input/output for TFLite
        // Assuming model takes [1, sequence_length, features] or [1, features]
        // Let's assume input shape is [1, 5] based on the Dashboard mock (5 fake points)
        val input = arrayOf(normalized) 
        val output = Array(1) { FloatArray(1) }

        interpreter?.run(input, output)

        val prediction = output[0][0] * 100 // Scale back to mg/dL range (example)
        val risk = RiskDetector.detectRisk(prediction)

        PredictionResult(prediction, risk)
    }
    
    suspend fun savePrediction(value: Float) = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()
        
        val entry = LogEntry(
            date = sdf.format(now),
            session = "AI Prediction",
            medType = "AI Automated",
            time = timeSdf.format(now),
            bgBefore = value.toDouble() / 18.0, // Convert mg/dL to mmol/L
            note = "AI Predicted value: ${String.format("%.1f", value / 18.0)} mmol/L"
        )
        dao.upsert(entry)
    }
}
