package com.example.nhatkyduonghuyet.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.nhatkyduonghuyet.ai.LSTMEngine
import com.example.nhatkyduonghuyet.ai.Normalizer
import com.example.nhatkyduonghuyet.ai.PredictionOutcome
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.ai.RiskDetector
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LogEntryDao,
    private val model: LSTMEngine
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
    private val calibrationMutex = Mutex()

    @Volatile
    private var modelBias: Float = prefs.getFloat(PREF_MODEL_BIAS, 0f)

    suspend fun checkRetrainStatus(): Boolean = withContext(Dispatchers.IO) {
        val currentCount = dao.getTotalCount().first()
        val lastTrainedCount = prefs.getInt(PREF_LAST_TRAINED_COUNT, 0)

        if (currentCount >= lastTrainedCount + RETRAIN_INTERVAL) {
            exportDataForRetraining()
            prefs.edit().putInt(PREF_LAST_TRAINED_COUNT, currentCount).apply()
            return@withContext true
        }
        false
    }

    private suspend fun exportDataForRetraining() = withContext(Dispatchers.IO) {
        val entries = dao.getAllLogEntries().first()
        val csvHeader = "Ngày,Buổi,Loại insulin/thuốc,Liều (đv/viên),Giờ tiêm/uống,Đường huyết trước (mmol/L),Đường huyết sau 2 giờ (mmol/L),Triệu chứng/Ghi chú\n"
        val csvBody = entries.joinToString("\n") {
            "${it.date},${it.session},${it.medType ?: ""},${it.dose ?: ""},${it.time ?: ""},${it.bgBefore ?: ""},${it.bgAfter ?: ""},${it.note ?: ""}"
        }

        File(context.getExternalFilesDir(null), "glucose_latest.csv").writeText(csvHeader + csvBody)
    }

    suspend fun runPrediction(rawMmol: FloatArray): PredictionOutcome<PredictionResult> =
        withContext(Dispatchers.Default) {
            if (rawMmol.size != Normalizer.SEQUENCE_LENGTH || rawMmol.any { !Normalizer.isValidGlucose(it) }) {
                return@withContext PredictionOutcome.Failure(
                    "Cần đúng ${Normalizer.SEQUENCE_LENGTH} chỉ số hợp lệ để chạy dự đoán."
                )
            }

            try {
                val basePrediction = model.predict(Normalizer.toLstmInput(rawMmol))
                val correctedPrediction = (basePrediction + modelBias).coerceIn(
                    Normalizer.MIN_GLUCOSE_MMOL,
                    Normalizer.MAX_GLUCOSE_MMOL
                )
                PredictionOutcome.Success(
                    PredictionResult(
                        current = rawMmol.last(),
                        next = correctedPrediction,
                        trend = correctedPrediction - rawMmol.last(),
                        risk = RiskDetector.detectRisk(correctedPrediction)
                    )
                )
            } catch (_: Exception) {
                PredictionOutcome.Failure("Mô hình dự đoán hiện không khả dụng. Vui lòng thử lại sau.")
            }
        }

    /**
     * Updates the bias only after enough new, valid measurements are available.
     * Predicted rows and legacy fallback values are deliberately excluded.
     */
    suspend fun autoCalibrate(): Boolean {
        val measurements = withContext(Dispatchers.IO) {
            dao.getAllLogEntries().first()
                .asSequence()
                .filter { it.session != "AI Prediction" }
                .mapNotNull { entry -> entry.bgBefore?.toFloat()?.takeIf(Normalizer::isValidGlucose) }
                // The query returns newest-first. Take the most recent N entries,
                // then restore chronological order so every window predicts the
                // actual next measurement forward in time.
                .take(MAX_CALIBRATION_MEASUREMENTS)
                .toList()
                .asReversed()
        }

        return withContext(Dispatchers.Default) {
            calibrationMutex.withLock {
                val previousSampleCount = prefs.getInt(PREF_LAST_CALIBRATION_SAMPLE_COUNT, 0)
                if (
                    measurements.size < MIN_CALIBRATION_MEASUREMENTS ||
                    measurements.size < previousSampleCount + MIN_NEW_MEASUREMENTS_FOR_CALIBRATION
                ) {
                    return@withLock false
                }

                val errors = buildList {
                    for (index in Normalizer.SEQUENCE_LENGTH until measurements.size) {
                        val history = measurements.subList(index - Normalizer.SEQUENCE_LENGTH, index).toFloatArray()
                        val actual = measurements[index]
                        try {
                            val predicted = model.predict(Normalizer.toLstmInput(history))
                            val error = actual - predicted
                            if (error.isFinite() && error in -MAX_CALIBRATION_ERROR..MAX_CALIBRATION_ERROR) {
                                add(error)
                            }
                        } catch (_: Exception) {
                            // Keep the previous calibration if a model inference cannot be completed.
                        }
                    }
                }

                if (errors.size < MIN_CALIBRATION_WINDOWS) return@withLock false

                val meanError = errors.average().toFloat()
                modelBias = ((modelBias * BIAS_SMOOTHING) + (meanError * (1f - BIAS_SMOOTHING)))
                    .coerceIn(-MAX_ABSOLUTE_BIAS, MAX_ABSOLUTE_BIAS)
                prefs.edit()
                    .putFloat(PREF_MODEL_BIAS, modelBias)
                    .putInt(PREF_LAST_CALIBRATION_SAMPLE_COUNT, measurements.size)
                    .apply()
                true
            }
        }
    }

    suspend fun savePrediction(predictionMmol: Float) = withContext(Dispatchers.IO) {
        require(Normalizer.isValidGlucose(predictionMmol)) { "Không thể lưu dự đoán ngoài khoảng hợp lệ." }
        val now = Date()
        val entry = LogEntry(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
            session = "AI Prediction",
            medType = "AI Automated",
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
            bgBefore = predictionMmol.toDouble(),
            note = "AI Predicted: ${String.format(Locale.getDefault(), "%.1f", predictionMmol)} mmol/L"
        )
        dao.upsert(entry)
    }

    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }

    private companion object {
        const val PREF_MODEL_BIAS = "model_bias"
        const val PREF_LAST_TRAINED_COUNT = "last_trained_count"
        const val PREF_LAST_CALIBRATION_SAMPLE_COUNT = "last_calibration_sample_count"
        const val RETRAIN_INTERVAL = 50
        const val MAX_CALIBRATION_MEASUREMENTS = 40
        const val MIN_CALIBRATION_MEASUREMENTS = 15
        const val MIN_NEW_MEASUREMENTS_FOR_CALIBRATION = 5
        const val MIN_CALIBRATION_WINDOWS = 10
        const val MAX_CALIBRATION_ERROR = 12f
        const val MAX_ABSOLUTE_BIAS = 5f
        const val BIAS_SMOOTHING = 0.7f
    }
}