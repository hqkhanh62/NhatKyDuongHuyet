package com.example.nhatkyduonghuyet.domain.health

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.GlucosePolicy

/**
 * Toán học chỉ số dùng chung giữa Dashboard và luồng quét camera AI.
 *
 * Trước đây công thức HbA1c và cách lọc dữ liệu bị nhân bản ở hai nơi nên dễ
 * lệch nhau; từ khi có vòng "Quét -> Lưu -> Dự báo -> Cảnh báo", cả hai bên
 * phải tính trên cùng một định nghĩa.
 */
object GlucoseMetrics {

    /** Bản ghi do mô hình tạo ra, không được tính vào trung bình/HbA1c. */
    const val SESSION_AI_PREDICTION = "AI Prediction"

    /**
     * Bản ghi giả dùng để tính HbA1c *ước tính trước khi lưu* (chỉ tồn tại
     * trong bộ nhớ phiên quét, không bao giờ được ghi xuống database).
     */
    const val SESSION_PENDING_READING = "Pending scan"

    /** Ngưỡng HbA1c mục tiêu theo đa số phác đồ điều trị đái tháo đường. */
    const val HBA1C_TARGET_PERCENT = 7.0

    /** Khoảng đường huyết trong mục tiêu (TIR) khi tính phần trăm thời gian. */
    const val TIR_MIN_MMOL = 3.9f
    const val TIR_MAX_MMOL = 10.0f

    fun isValidMmol(value: Double): Boolean =
        value.isFinite() &&
            value >= GlucosePolicy.MIN_GLUCOSE_MMOL.toDouble() &&
            value <= GlucosePolicy.MAX_GLUCOSE_MMOL.toDouble()

    /** Hai ô chỉ số của một bản ghi ngày/buổi. */
    fun readingsOf(entry: LogEntry): List<Double> =
        listOfNotNull(entry.bgBefore, entry.bgAfter).filter { isValidMmol(it) }

    /** Trung bình từng ngày, theo thứ tự thời gian tăng dần. */
    fun dailyMeasuredAverages(entries: List<LogEntry>): Map<String, Float> =
        entries.groupBy { it.date }
            .toSortedMap()
            .mapValues { (_, dayEntries) ->
                dayEntries.flatMap { readingsOf(it) }.average().toFloat()
            }
            .filterValues { it.isFinite() && it > 0f }

    /**
     * Trung bình ngày khi đã cộng thêm một chỉ số vừa quét (chưa lưu).
     * Dùng cho banner cảnh báo tức thì để con số trên camera khớp với
     * Dashboard ngay sau khi xác nhận.
     */
    fun dailyAveragesWithReading(entries: List<LogEntry>, date: String, value: Float): Map<String, Float> {
        val pending = LogEntry(
            id = -1L,
            date = date,
            session = SESSION_PENDING_READING,
            bgBefore = value.toDouble()
        )
        return dailyMeasuredAverages(entries + pending)
    }

    /** Dãy chỉ số hợp lệ theo thứ tự thời gian, phục vụ LSTM/buffer realtime. */
    fun chronologicalMeasurements(entries: List<LogEntry>): List<Float> =
        entries.asSequence()
            .filter { it.session != SESSION_AI_PREDICTION }
            .sortedWith(compareBy<LogEntry> { it.date }.thenBy { it.time ?: "" })
            .flatMap { entry -> readingsOf(entry).map { it.toFloat() } }
            .filter { GlucosePolicy.isValid(it) }
            .toList()

    /** Trung bình có trọng số: ngày gần đây ảnh hưởng mạnh hơn. */
    fun weightedAverage(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        var sum = 0.0
        var weightSum = 0f
        values.forEachIndexed { index, value ->
            val weight = (index + 1).toFloat()
            sum += value * weight
            weightSum += weight
        }
        return if (weightSum <= 0f) 0f else (sum / weightSum).toFloat()
    }

    /**
     * HbA1c ước tính từ đường huyết trung bình (mmol/L).
     * Công thức chuyển đổi: NGSP% = (ADAG_mg/dL + 46.7) / 28.7, với
     * ADAG_mg/dL = (mmol/L * 18.0182). Rút gọn thành (avg + 2.59) / 1.59.
     */
    fun estimateHba1c(averageMmol: Float): Double {
        if (!averageMmol.isFinite() || averageMmol <= 0f) return 0.0
        return ((averageMmol + 2.59f) / 1.59f).toDouble()
    }

    fun timeInRangePercent(values: List<Float>): Int? {
        if (values.isEmpty()) return null
        val inRange = values.count { it >= TIR_MIN_MMOL && it <= TIR_MAX_MMOL }
        return inRange * 100 / values.size
    }
}

/** Kết quả ước tính HbA1c, dùng cho vòng AI realtime sau mỗi lần quét. */
data class Hba1cEstimate(
    val percent: Double,
    val weightedAverageMmol: Float,
    val dayCount: Int
) {
    val isAvailable: Boolean get() = percent > 0.0
    val isAboveTarget: Boolean get() = isAvailable && percent > GlucoseMetrics.HBA1C_TARGET_PERCENT
}
