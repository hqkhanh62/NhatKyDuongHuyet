package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import javax.inject.Inject

class PredictGlucose @Inject constructor() {
    operator fun invoke(logs: List<LogEntry>): String {
        if (logs.size < 5) return "Chưa đủ dữ liệu dự báo"
        
        val allValues = logs.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
        if (allValues.isEmpty()) return "Chưa có dữ liệu đường huyết"
        
        val avg = allValues.average()

        return when {
            avg > 10.0 -> "⚠️ Đường huyết trung bình cao – Hãy chú ý ăn uống và vận động"
            avg < 4.0 -> "⚠️ Nguy cơ hạ đường huyết – Hãy bổ sung đường ngay"
            else -> "✅ Chỉ số ổn định – Tiếp tục duy trì chế độ hiện tại"
        }
    }
}
