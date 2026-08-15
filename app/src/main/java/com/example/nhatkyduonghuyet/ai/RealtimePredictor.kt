package com.example.nhatkyduonghuyet.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RealtimePredictor(
    private val model: LSTMEngine
) {
    private val buffer = GlucoseBuffer()
    private val bufferMutex = Mutex()

    suspend fun refresh(history: List<Float>): PredictionOutcome<RealtimeForecast> =
        withContext(Dispatchers.Default) {
            bufferMutex.withLock {
                buffer.clear()
                history.takeLast(Normalizer.SEQUENCE_LENGTH).forEach { value ->
                    if (!Normalizer.isValidGlucose(value)) {
                        return@withLock PredictionOutcome.Failure("Lịch sử có chỉ số đường huyết không hợp lệ.")
                    }
                    buffer.add(value)
                }
                buildForecastLocked()
            }
        }

    suspend fun onNewGlucose(value: Float): PredictionOutcome<PredictionResult> =
        withContext(Dispatchers.Default) {
            bufferMutex.withLock {
                if (!Normalizer.isValidGlucose(value)) {
                    return@withLock PredictionOutcome.Failure("Chỉ số quét không nằm trong khoảng hợp lệ.")
                }
                buffer.add(value)
                buildNextPredictionLocked()
            }
        }

    suspend fun predictFuture24Hours(): PredictionOutcome<MultiStepResult> =
        withContext(Dispatchers.Default) {
            bufferMutex.withLock { buildFutureLocked() }
        }

    private fun buildForecastLocked(): PredictionOutcome<RealtimeForecast> {
        return when (val next = buildNextPredictionLocked()) {
            is PredictionOutcome.Failure -> next
            is PredictionOutcome.Success -> when (val future = buildFutureLocked()) {
                is PredictionOutcome.Failure -> future
                is PredictionOutcome.Success -> PredictionOutcome.Success(RealtimeForecast(next.value, future.value))
            }
        }
    }

    private fun buildNextPredictionLocked(): PredictionOutcome<PredictionResult> {
        if (!buffer.isReady()) {
            return PredictionOutcome.Failure("Cần ít nhất ${Normalizer.SEQUENCE_LENGTH} lần đo hợp lệ để dự báo.")
        }
        return try {
            val current = buffer.getAll().last()
            val next = model.predict(buffer.toInputArray()).coerceIn(
                Normalizer.MIN_GLUCOSE_MMOL,
                Normalizer.MAX_GLUCOSE_MMOL
            )
            PredictionOutcome.Success(
                PredictionResult(
                    current = current,
                    next = next,
                    trend = next - current,
                    risk = RiskDetector.detectRisk(next)
                )
            )
        } catch (_: Exception) {
            PredictionOutcome.Failure("Không thể chạy mô hình dự báo LSTM.")
        }
    }

    private fun buildFutureLocked(): PredictionOutcome<MultiStepResult> {
        if (!buffer.isReady()) {
            return PredictionOutcome.Failure("Cần ít nhất ${Normalizer.SEQUENCE_LENGTH} lần đo hợp lệ để dự báo 24 giờ.")
        }
        return try {
            val values = buffer.getAll().toMutableList()
            val forecasts = buildList {
                repeat(4) {
                    val input = Normalizer.toLstmInput(values.takeLast(Normalizer.SEQUENCE_LENGTH).toFloatArray())
                    val next = model.predict(input).coerceIn(
                        Normalizer.MIN_GLUCOSE_MMOL,
                        Normalizer.MAX_GLUCOSE_MMOL
                    )
                    add(next)
                    values.add(next)
                }
            }
            PredictionOutcome.Success(
                MultiStepResult(
                    hourlyForecasts = forecasts,
                    maxExpected = forecasts.max(),
                    minExpected = forecasts.min()
                )
            )
        } catch (_: Exception) {
            PredictionOutcome.Failure("Không thể tạo dự báo 24 giờ từ mô hình LSTM.")
        }
    }
}