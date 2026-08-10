package com.example.nhatkyduonghuyet.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.nhatkyduonghuyet.ai.*
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
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
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)

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

    suspend fun checkRetrainStatus(): Boolean = withContext(Dispatchers.IO) {
        val currentCount = dao.getTotalCount().first()
        val lastTrainedCount = prefs.getInt("last_trained_count", 0)
        
        if (currentCount >= lastTrainedCount + 50) {
            exportDataForRetraining()
            prefs.edit().putInt("last_trained_count", currentCount).apply()
            return@withContext true
        }
        return@withContext false
    }

    private suspend fun exportDataForRetraining() = withContext(Dispatchers.IO) {
        val entries = dao.getAllLogEntries().first()
        val csvHeader = "Ngày,Buổi,Loại insulin/thuốc,Liều (đv/viên),Giờ tiêm/uống,Đường huyết trước (mmol/L),Đường huyết sau 2 giờ (mmol/L),Triệu chứng/Ghi chú\n"
        val csvBody = entries.joinToString("\n") { 
            "${it.date},${it.session},${it.medType ?: ""},${it.dose ?: ""},${it.time ?: ""},${it.bgBefore ?: ""},${it.bgAfter ?: ""},${it.note ?: ""}"
        }
        
        val file = File(context.getExternalFilesDir(null), "glucose_latest.csv")
        file.writeText(csvHeader + csvBody)
    }

    suspend fun runPrediction(rawMmol: FloatArray): PredictionResult = withContext(Dispatchers.Default) {
        val normalized = Normalizer.normalize(rawMmol)
        
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
            note = "AI Predicted: ${String.format(Locale.getDefault(), "%.1f", predictionMmol)} mmol/L"
        )
        dao.upsert(entry)
    }

    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
    }
}
