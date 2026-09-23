package com.example.nhatkyduonghuyet.data.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.nhatkyduonghuyet.reminder.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Writes a full backup bundle into the user's chosen folder once a week.
 *
 * This is the safety net for the common case: someone who reads the reminder,
 * means to export, and never gets around to it. It only runs when the user has
 * explicitly granted a folder, and it skips itself if they exported by hand
 * recently.
 *
 * The other workers in this app are plain WorkManager workers rather than
 * @HiltWorker ones, so this follows the same pattern and pulls its dependency
 * through an entry point - that avoids adding hilt-work and a custom
 * WorkerFactory just for one job.
 */
class AutoExportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupEntryPoint {
        fun backupRepository(): BackupRepository
    }

    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors
            .fromApplication(applicationContext, BackupEntryPoint::class.java)
            .backupRepository()

        if (!repository.isAutoExportEnabled) return Result.success()

        return when (val outcome = repository.runScheduledExport()) {
            is BackupResult.Success -> {
                Log.i(TAG, outcome.message)
                Result.success()
            }
            is BackupResult.Failure -> {
                Log.w(TAG, outcome.message)
                // Tell the user, otherwise a silently broken automatic backup is
                // worse than none - they would believe they are covered.
                NotificationHelper.createNotificationChannel(applicationContext)
                NotificationHelper.showNotification(
                    applicationContext,
                    "Sao lưu tự động thất bại",
                    "${outcome.message} Hãy mở app và xuất file thủ công."
                )
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "AutoExportWorker"
        private const val WORK_NAME = "auto_export_weekly"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoExportWorker>(7, TimeUnit.DAYS)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.HOURS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // KEEP, so toggling the setting does not reset the week's timer.
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
