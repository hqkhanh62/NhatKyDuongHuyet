package com.example.nhatkyduonghuyet.reminder

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Lịch đo định kỳ tùy chỉnh, bắt đầu từ ngày được lưu làm mốc. */
object GlucoseMeasurementSchedule {
    const val DEFAULT_INTERVAL_DAYS = 4
    const val MIN_INTERVAL_DAYS = 1
    const val MAX_INTERVAL_DAYS = 30
    const val SESSION_KEY = "GLUCOSE_PERIODIC"
    const val SESSION_LABEL = "Đo đường huyết định kỳ"
    const val REMINDER_HOUR = 8
    const val REMINDER_MINUTE = 0

    private const val PREFS_NAME = "glucose_measurement_schedule"
    private const val KEY_ANCHOR_DATE = "anchor_date"
    private const val KEY_INTERVAL_DAYS = "interval_days"
    private const val DATE_PATTERN = "yyyy-MM-dd"

    fun ensureStarted(context: Context, now: Date = Date()): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_ANCHOR_DATE, null)
        if (!existing.isNullOrBlank()) return existing

        val anchor = formatDate(now)
        prefs.edit().putString(KEY_ANCHOR_DATE, anchor).apply()
        return anchor
    }

    fun anchorDate(context: Context): String? = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_ANCHOR_DATE, null)

    fun intervalDays(context: Context): Int = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_INTERVAL_DAYS, DEFAULT_INTERVAL_DAYS)
        .coerceIn(MIN_INTERVAL_DAYS, MAX_INTERVAL_DAYS)

    /** Lưu chu kỳ mới và bắt đầu lại chu kỳ từ hôm nay. */
    fun setIntervalDays(context: Context, days: Int, now: Date = Date()): Int {
        val normalized = days.coerceIn(MIN_INTERVAL_DAYS, MAX_INTERVAL_DAYS)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL_DAYS, normalized)
            .putString(KEY_ANCHOR_DATE, formatDate(now))
            .apply()
        return normalized
    }

    /** Trả về ngày đo gần nhất, bao gồm hôm nay, tính theo mốc ban đầu. */
    fun nextDueDate(context: Context, now: Date = Date()): Date {
        val anchor = parseDate(ensureStarted(context, now))
        val today = startOfDay(now)
        val due = Calendar.getInstance().apply { time = anchor }
        while (due.time.before(today)) {
            due.add(Calendar.DAY_OF_MONTH, intervalDays(context))
        }
        return due.time
    }

    fun isDueToday(context: Context, now: Date = Date()): Boolean =
        formatDate(nextDueDate(context, now)) == formatDate(now)

    fun widgetText(context: Context, now: Date = Date()): String {
        val due = nextDueDate(context, now)
        val today = startOfDay(now)
        val label = SimpleDateFormat("dd/MM", Locale.getDefault()).format(due)
        return if (due.time == today.time) {
            "Đến lịch đo hôm nay"
        } else {
            "Lần đo tiếp theo: $label"
        }
    }

    fun dateTimeForNextDue(context: Context, now: Date = Date()): Date {
        val due = Calendar.getInstance().apply {
            time = nextDueDate(context, now)
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (due.timeInMillis <= now.time) {
            due.add(Calendar.DAY_OF_MONTH, intervalDays(context))
        }
        return due.time
    }

    private fun formatDate(date: Date): String =
        SimpleDateFormat(DATE_PATTERN, Locale.US).format(date)

    private fun parseDate(value: String): Date =
        SimpleDateFormat(DATE_PATTERN, Locale.US).parse(value) ?: Date()

    private fun startOfDay(date: Date): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}
