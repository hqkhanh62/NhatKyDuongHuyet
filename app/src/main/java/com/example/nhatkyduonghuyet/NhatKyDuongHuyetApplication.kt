package com.example.nhatkyduonghuyet

import android.app.Application
import com.example.nhatkyduonghuyet.reminder.NotificationHelper
import com.example.nhatkyduonghuyet.reminder.ReminderScheduler

class NhatKyDuongHuyetApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        NotificationHelper.createNotificationChannel(this)
        ReminderScheduler.scheduleAllReminders(this)
    }
}
