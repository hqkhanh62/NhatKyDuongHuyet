package com.example.nhatkyduonghuyet.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderScheduler {

    private const val ACTION_TRIGGER_REMINDER = "com.example.nhatkyduonghuyet.reminder.ACTION_TRIGGER_REMINDER"

    fun scheduleDailyReminder(
        context: Context,
        sessionKey: String,
        sessionLabel: String,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra("SESSION_KEY", sessionKey)
            putExtra("SESSION_LABEL", sessionLabel)
            putExtra("REMINDER_TIME", String.format("%02d:%02d", hour, minute))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionKey.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Use setExactAndAllowWhileIdle for P1-02 compliance (Exact timing)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            dueTime.timeInMillis,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context, sessionKey: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionKey.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleAllReminders(context: Context) {
        val defaultReminders = listOf(
            Triple("SANG", "Sáng", "06:30"),
            Triple("TRUA", "Trưa", "11:30"),
            Triple("CHIEU_TOI", "Chiều/Tối", "17:30"),
            Triple("TRUOC_NGU", "Trước ngủ", "22:00")
        )

        defaultReminders.forEach { (key, label, time) ->
            val (hour, minute) = time.split(":").map { it.toInt() }
            scheduleDailyReminder(context, key, label, hour, minute)
        }
    }

    fun cancelAllReminders(context: Context) {
        val defaultReminders = listOf("SANG", "TRUA", "CHIEU_TOI", "TRUOC_NGU")
        defaultReminders.forEach { key ->
            cancelReminder(context, key)
        }
    }
}
