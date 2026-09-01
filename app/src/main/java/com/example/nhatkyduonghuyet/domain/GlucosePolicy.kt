package com.example.nhatkyduonghuyet.domain

object GlucosePolicy {
    const val MIN_GLUCOSE_MMOL = 2.0f
    const val MAX_GLUCOSE_MMOL = 30.0f
    
    const val LOW_THRESHOLD = 4.0f
    const val HIGH_THRESHOLD = 10.0f
    const val VERY_HIGH_THRESHOLD = 13.0f

    fun isValid(value: Float): Boolean =
        value.isFinite() && value in MIN_GLUCOSE_MMOL..MAX_GLUCOSE_MMOL
}

enum class GlucoseRiskLevel(val label: String, val colorCode: String) {
    LOW("Nguy cơ hạ đường huyết", "#FF5252"),
    NORMAL("Bình thường", "#4CAF50"),
    HIGH("Đường huyết cao", "#FF9800"),
    VERY_HIGH("Nguy cơ biến chứng", "#D32F2F")
}
