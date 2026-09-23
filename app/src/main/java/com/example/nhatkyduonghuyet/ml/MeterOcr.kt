package com.example.nhatkyduonghuyet.ml

import java.util.Locale

/*
 * Dữ liệu OCR "đa tầng" đọc được từ màn hình máy đo.
 *
 * Tách riêng khỏi [GlucoseScanner] để toàn bộ việc phân tích văn bản là Kotlin
 * thuần (không import Android/ML Kit) và chạy được trên JVM unit test.
 */

/** Một dòng văn bản ML Kit trả về, kèm chiều cao khung chữ để so sánh cỡ chữ. */
data class OcrLine(
    val text: String,
    val heightPx: Int = 0
)

/** Chỉ số đường huyết trích xuất được từ màn hình. */
data class GlucoseReading(
    val value: Float,
    val confidence: Float,
    /** Dòng thắng cuộc đến từ cây layout của ML Kit (có thông tin cỡ chữ). */
    val fromSpatialLine: Boolean,
    /** Màn hình có ghi đơn vị mmol/L hoặc mg/dL ngay trên dòng chứa chỉ số. */
    val hasUnit: Boolean,
    /** Dấu thập phân được nhìn thấy rõ (không phải do suy đoán). */
    val hasDecimal: Boolean
) {
    val isHighConfidence: Boolean get() = confidence >= HIGH_CONFIDENCE

    private companion object {
        const val HIGH_CONFIDENCE = 0.75f
    }
}

/** Giờ HH:mm hiển thị trên màn hình máy đo. */
data class MeterTime(
    val hour: Int,
    val minute: Int,
    val confidence: Float = 0f
) {
    val formatted: String get() = String.format(Locale.US, "%02d:%02d", hour, minute)
}

/** Ngày trên màn hình máy đo, đã chuẩn hoá về yyyy-MM-dd. */
data class MeterDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val confidence: Float = 0f,
    /** DD/MM và MM/DD hoán vị được cho nhau -> AI không chắc, người dùng nên kiểm. */
    val ambiguous: Boolean = false
) {
    val iso: String get() = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    val dayMonth: String get() = String.format(Locale.US, "%02d/%02d", day, month)
}

/**
 * Toàn bộ những gì AI nhìn thấy trên một frame của màn hình máy đo:
 * chỉ số (tầng 1), giờ (tầng 2), ngày (tầng 3) và mã lỗi máy đo.
 */
data class MeterDisplayFields(
    val glucose: GlucoseReading? = null,
    val time: MeterTime? = null,
    val date: MeterDate? = null,
    val errorCode: String? = null,
    val rawText: String = "",
    val lines: List<OcrLine> = emptyList(),
    /** Da chay them luot quet toan bo chieu cao man hinh de tim dong chu nho. */
    val smallTextScanned: Boolean = false
) {
    val value: Float? get() = glucose?.value
    val hasReading: Boolean get() = glucose != null
    val hasError: Boolean get() = errorCode != null

    /** Văn bản nhận dạng được, rút gọn để hiển thị trong banner. */
    val readableText: String
        get() = lines.map { it.text.trim() }.filter { it.isNotEmpty() }.joinToString(" | ")
            .ifBlank { rawText.replace('\n', ' ').trim() }
}
