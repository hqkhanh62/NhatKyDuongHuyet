package com.example.nhatkyduonghuyet.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                NotificationHelper.createNotificationChannel(context)
                ReminderScheduler.scheduleAllReminders(context)
            }
            "com.example.nhatkyduonghuyet.reminder.ACTION_TRIGGER_REMINDER" -> {
                val label = intent.getStringExtra("SESSION_LABEL") ?: "Nhắc nhở"
                val time = intent.getStringExtra("REMINDER_TIME") ?: ""
                
                NotificationHelper.showNotification(
                    context,
                    "Nhắc nhở uống thuốc & đo đường huyết",
                    "Đã đến giờ $label ($time). Hãy ghi nhật ký sức khỏe của bạn."
                )
                
                // Reschedule for the next day
                ReminderScheduler.scheduleAllReminders(context)
            }
        }
    }
}
