package com.example.nhatkyduonghuyet.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val session = inputData.getString("SESSION_KEY") ?: ""
        val reminderTime = inputData.getString("REMINDER_TIME") ?: ""

        NotificationHelper.showNotification(
            applicationContext,
            "Nhắc nhở ghi nhật ký đường huyết",
            "Đã đến giờ ghi nhật ký đường huyết cho buổi $session ($reminderTime)!"
        )
        return Result.success()
    }
}
