package com.example.nhatkyduonghuyet.domain.scanner

/**
 * Bốn buổi trong nhật ký ngày. Nhãn trùng tên 4 thẻ của `DayDetailScreen`
 * để bản ghi do camera tự tạo luôn nằm đúng ô người dùng nhìn thấy.
 */
enum class GlucoseSession(val label: String, val shortLabel: String) {
    MORNING("Sáng", "Buổi sáng"),
    NOON("Trưa", "Buổi trưa"),
    AFTERNOON("Chiều", "Buổi chiều"),
    EVENING("Tối", "Buổi tối");

    /**
     * Mốc giờ ăn (phút trong ngày) dùng để suy ra ô "trước ăn"/"sau ăn".
     * Buổi Chiều được neo theo bữa trưa vì chỉ số chiều thường là chỉ số
     * sau ăn trưa 2 giờ của người bệnh.
     */
    val mealAnchorMinutes: Int
        get() = when (this) {
            MORNING -> 6 * 60 + 30
            NOON -> 11 * 60 + 30
            AFTERNOON -> 11 * 60 + 30
            EVENING -> 18 * 60 + 30
        }

    companion object {
        /** Tự phân loại buổi theo giờ AI vừa quét được (hoặc giờ hệ thống). */
        fun fromHour(hour: Int): GlucoseSession = when (hour) {
            in 5..10 -> MORNING
            in 11..13 -> NOON
            in 14..17 -> AFTERNOON
            else -> EVENING // 18:00 -> 04:59
        }

        fun fromTime(time: String?): GlucoseSession? {
            val hour = time?.substringBefore(':')?.toIntOrNull() ?: return null
            if (hour !in 0..23) return null
            return fromHour(hour)
        }

        fun fromLabel(label: String?): GlucoseSession? =
            values().firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) }
    }
}

/** Ô giá trị trong một buổi: trước ăn (chỉ số nền) hoặc sau ăn 2 giờ. */
enum class ReadingSlot(val label: String) {
    BEFORE_MEAL("Trước ăn"),
    AFTER_MEAL("Sau ăn 2 giờ");

    val isBefore: Boolean get() = this == BEFORE_MEAL
}

/** Nguồn gốc của một trường dữ liệu: đọc từ máy đo hay lấy dự phòng từ máy điện thoại. */
enum class FieldSource(val label: String) {
    METER("từ máy đo"),
    SYSTEM("giờ hệ thống"),
    MANUAL("người dùng sửa")
}
