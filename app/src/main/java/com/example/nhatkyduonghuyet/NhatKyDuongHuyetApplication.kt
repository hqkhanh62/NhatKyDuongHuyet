package com.example.nhatkyduonghuyet

import android.app.Application
import android.util.Log
import com.example.nhatkyduonghuyet.data.repository.MedicationBackupRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NhatKyDuongHuyetApplication : Application() {

    @Inject
    lateinit var backupRepository: MedicationBackupRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        appScope.launch {
            runCatching {
                // Dated snapshot first: the Room database uses
                // fallbackToDestructiveMigration(), so an app update with a
                // schema bump would otherwise drop the medication history.
                backupRepository.backupIfAppUpdated(currentVersionName())
            }.onFailure { Log.e(TAG, "Sao luu khi cap nhat app that bai", it) }

            // Then keep a rolling snapshot in sync with every change.
            backupRepository.startAutoBackup(appScope)
        }
    }

    private fun currentVersionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown")

    private companion object {
        const val TAG = "NhatKyApp"
    }
}
