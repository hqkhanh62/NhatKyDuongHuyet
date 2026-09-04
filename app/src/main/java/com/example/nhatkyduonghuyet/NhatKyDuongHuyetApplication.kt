package com.example.nhatkyduonghuyet

import android.app.Application
import android.util.Log
import com.example.nhatkyduonghuyet.data.backup.BackupRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NhatKyDuongHuyetApplication : Application() {

    @Inject
    lateinit var backupRepository: BackupRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        appScope.launch {
            runCatching {
                // Dated snapshot before the new version touches anything.
                // Room migrations preserve data now, but this is the cheap
                // insurance against a future migration shipping broken.
                backupRepository.backupIfAppUpdated(currentVersionName())
            }.onFailure { Log.e(TAG, "Sao luu khi cap nhat app that bai", it) }

            // Then mirror every table on every change.
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
