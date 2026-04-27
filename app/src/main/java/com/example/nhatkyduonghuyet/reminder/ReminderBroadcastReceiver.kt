package com.example.nhatkyduonghuyet.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            // Re-schedule all reminders after device reboot
            NotificationHelper.createNotificationChannel(context)
            ReminderScheduler.scheduleAllReminders(context)
        }
    }
}
