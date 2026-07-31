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

    suspend fun runPrediction(rawMmol: FloatArray): PredictionResult = withContext(Dispatchers.Default) {
        val normalized = Normalizer.normalize(rawMmol)
        
        // Input: [1, 5, 1] for LSTM
        val input = Array(1) { Array(5) { FloatArray(1) } }
        for (i in 0 until 5) {
            input[0][i][0] = if (i < normalized.size) normalized[i] else 0f
        }
        
        val output = Array(1) { FloatArray(1) }

        interpreter?.run(input, output)

        val predictionMmol = Normalizer.denormalize(output[0][0])
        val risk = RiskDetector.detectRisk(predictionMmol)

        PredictionResult(
            current = rawMmol.lastOrNull() ?: 0f,
            next = predictionMmol,
            trend = if (rawMmol.isNotEmpty()) predictionMmol - rawMmol.last() else 0f,
            risk = risk
        )
    }
    
    suspend fun savePrediction(predictionMmol: Float) = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()
        
        val entry = LogEntry(
            date = sdf.format(now),
            session = "AI Prediction",
            medType = "AI Automated",
            time = timeSdf.format(now),
            bgBefore = predictionMmol.toDouble(),
            note = "AI Predicted value: ${String.format(Locale.getDefault(), "%.1f", predictionMmol)} mmol/L"
        )
        dao.upsert(entry)
    }
}
