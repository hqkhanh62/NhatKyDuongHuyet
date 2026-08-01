package com.example.nhatkyduonghuyet.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val REMINDER_WORK_TAG_PREFIX = "daily_reminder_"

    fun scheduleDailyReminder(
        context: Context,
        sessionKey: String,
        sessionLabel: String,
        hour: Int,
        minute: Int
    ) {
        val now = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = dueTime.timeInMillis - now.timeInMillis

        val inputData = Data.Builder()
            .putString("SESSION_KEY", sessionLabel)
            .putString("REMINDER_TIME", String.format("%02d:%02d", hour, minute))
            .build()

        val dailyReminderWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(REMINDER_WORK_TAG_PREFIX + sessionKey)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK_TAG_PREFIX + sessionKey,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyReminderWorkRequest
        )
    }

    fun cancelReminder(context: Context, sessionKey: String) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_TAG_PREFIX + sessionKey)
    }

    fun scheduleAllReminders(context: Context) {
        // Default reminder times as per JSON spec
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
