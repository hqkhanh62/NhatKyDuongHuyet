package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.ai.PredictionOutcome
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.ai.RealtimePredictor
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import javax.inject.Inject

class PredictGlucose @Inject constructor(
    private val realtimePredictor: RealtimePredictor
) {
    /**
     * Thực hiện dự báo dựa trên chuỗi thời gian LSTM
     * @param logs Danh sách nhật ký để lấy chuỗi 5 điểm gần nhất
     */
    suspend operator fun invoke(logs: List<LogEntry>): PredictionResult? {
        if (logs.isEmpty()) return null

        // Lấy tất cả giá trị đường huyết thực tế theo trình tự thời gian
        val glucoseValues = logs
            .flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
            .takeLast(5)
            .map { it.toFloat() }

        if (glucoseValues.size < 5) return null

        // Chạy dự báo thông qua bộ nạp mô hình LSTM
        return when (val outcome = realtimePredictor.onNewGlucose(glucoseValues.last())) {
            is PredictionOutcome.Success -> outcome.value
            is PredictionOutcome.Failure -> null
        }
    }
}
