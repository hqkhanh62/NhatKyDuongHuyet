package com.example.nhatkyduonghuyet.scan

import java.util.Calendar

/**
 * Auto-classifies the diary session (buổi) from the hour the AI scanned.
 *
 * Boundaries match the rest of the app:
 * - Sáng: 00:00 – 09:59
 * - Trưa: 10:00 – 15:59
 * - Chiều: 16:00 – 19:59
 * - Tối: 20:00 – 23:59
 */
object SessionClassifier {

    const val MORNING = "Sáng"
    const val NOON = "Trưa"
    const val AFTERNOON = "Chiều"
    const val EVENING = "Tối"

    fun fromHour(hour: Int): String = when {
        hour < 0 || hour > 23 -> EVENING
        hour < 10 -> MORNING
        hour in 10..15 -> NOON
        hour in 16..19 -> AFTERNOON
        else -> EVENING
    }

    /**
     * Classifies from an `HH:mm` string. Falls back to the current system
     * hour when the text is missing or malformed.
     */
    fun fromTime(time: String?): String {
        val hour = time
            ?.substringBefore(':')
            ?.trim()
            ?.toIntOrNull()
        return if (hour != null && hour in 0..23) {
            fromHour(hour)
        } else {
            fromHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        }
    }
}
