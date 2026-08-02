package com.example.nhatkyduonghuyet.domain.usecase

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
    operator fun invoke(logs: List<LogEntry>): PredictionResult? {
        if (logs.isEmpty()) return null

        // Lấy tất cả giá trị đường huyết thực tế theo trình tự thời gian
        val glucoseValues = logs
            .flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
            .takeLast(5)
            .map { it.toFloat() }

        if (glucoseValues.size < 5) return null

        // Chạy dự báo thông qua bộ nạp mô hình LSTM
        // Chúng ta giả định giá trị cuối cùng trong chuỗi là 'current'
        // và LSTM sẽ dự báo giá trị 'next'
        
        // Lưu ý: RealtimePredictor đã có buffer nội bộ, 
        // nhưng ở UseCase này chúng ta có thể truyền chuỗi trực tiếp nếu cần tùy biến cao hơn.
        // Ở đây ta dùng hàm onNewGlucose để cập nhật điểm mới nhất và nhận dự báo.
        return realtimePredictor.onNewGlucose(glucoseValues.last())
    }
}
