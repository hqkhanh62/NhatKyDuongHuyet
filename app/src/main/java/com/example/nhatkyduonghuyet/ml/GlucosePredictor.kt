package com.example.nhatkyduonghuyet.ml

import android.content.Context
import android.util.Log
import com.example.nhatkyduonghuyet.ai.Normalizer
import com.example.nhatkyduonghuyet.ai.PredictionOutcome
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val inferenceMutex = Mutex()
    private var interpreter: Interpreter? = null

    init {
        try {
            interpreter = Interpreter(loadModelFile(context, "glucose_model.tflite"))
        } catch (error: Exception) {
            Log.e(TAG, "Model load failed", error)
        }
    }

    suspend fun predict(fasting: Float, type: Int): PredictionOutcome<Float> =
        withContext(Dispatchers.Default) {
            if (!Normalizer.isValidGlucose(fasting)) {
                return@withContext PredictionOutcome.Failure("Nhập chỉ số từ 2.0 đến 25.0 mmol/L.")
            }
            if (type !in 0..1) {
                return@withContext PredictionOutcome.Failure("Thời điểm dự đoán không hợp lệ.")
            }

            val activeInterpreter = interpreter
                ?: return@withContext PredictionOutcome.Failure("Mô hình dự đoán không tải được trên thiết bị này.")

            inferenceMutex.withLock {
                try {
                    val input = arrayOf(floatArrayOf(fasting, type.toFloat()))
                    val output = Array(1) { FloatArray(1) }
                    activeInterpreter.run(input, output)
                    val value = output[0][0]
                    if (!Normalizer.isValidGlucose(value)) {
                        PredictionOutcome.Failure("Mô hình trả về giá trị ngoài khoảng an toàn.")
                    } else {
                        PredictionOutcome.Success(value)
                    }
                } catch (error: Exception) {
                    Log.e(TAG, "Prediction failed", error)
                    PredictionOutcome.Failure("Không thể chạy mô hình dự đoán. Vui lòng thử lại sau.")
                }
            }
        }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer =
        context.assets.openFd(filename).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        }

    private companion object {
        const val TAG = "GlucosePredictor"
    }
}