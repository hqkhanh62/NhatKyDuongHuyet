package com.example.nhatkyduonghuyet.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.nhatkyduonghuyet.data.local.dao.MedicationDao
import com.example.nhatkyduonghuyet.util.MedicationBackupManager
import com.example.nhatkyduonghuyet.util.MedicationCsv
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a backup/export/restore operation, surfaced to the UI. */
sealed interface BackupOutcome {
    data class Success(val message: String) : BackupOutcome
    data class Failure(val message: String) : BackupOutcome
}

/**
 * Coordinates automatic CSV snapshots of the prescription and intake history.
 *
 * - Observes both tables and rewrites the rolling snapshot on every change.
 * - Writes an extra dated snapshot the first time the app runs after an update,
 *   before `fallbackToDestructiveMigration()` can throw the data away.
 */
@Singleton
class MedicationBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationDao: MedicationDao,
    private val backupManager: MedicationBackupManager
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _lastBackupAt = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP_AT, 0L))
    val lastBackupAt: Flow<Long> = _lastBackupAt.asStateFlow()

    /**
     * Starts the auto-backup loop. Safe to call once from Application.onCreate.
     * Changes are debounced so ticking several checkboxes writes one file.
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun startAutoBackup(scope: CoroutineScope) {
        combine(
            medicationDao.getAllMedications(),
            medicationDao.observeAllLogs()
        ) { meds, logs -> meds to logs }
            .debounce(AUTO_BACKUP_DEBOUNCE_MS)
            .onEach { (meds, logs) ->
                if (meds.isEmpty() && logs.isEmpty()) return@onEach
                withContext(Dispatchers.IO) {
                    backupManager.writeLatestSnapshot(meds, logs)
                        .onSuccess { markBackedUp() }
                }
            }
            .launchIn(scope)
    }

    /**
     * Writes a dated snapshot once per app version. Call this as early as
     * possible after startup, before any destructive migration can run.
     */
    suspend fun backupIfAppUpdated(currentVersion: String) = withContext(Dispatchers.IO) {
        val previous = prefs.getString(KEY_LAST_VERSION, null)
        if (previous == currentVersion) return@withContext

        val meds = medicationDao.getAllMedicationsOnce()
        val logs = medicationDao.getAllLogsOnce()
        if (meds.isNotEmpty() || logs.isNotEmpty()) {
            backupManager.writeVersionedSnapshot(currentVersion, meds, logs)
                .onSuccess {
                    Log.i(TAG, "Da sao luu CSV cho phien ban $currentVersion")
                    markBackedUp()
                }
        }
        prefs.edit().putString(KEY_LAST_VERSION, currentVersion).apply()
    }

    /** Manual "back up now" action. */
    suspend fun backupNow(): BackupOutcome = withContext(Dispatchers.IO) {
        val meds = medicationDao.getAllMedicationsOnce()
        val logs = medicationDao.getAllLogsOnce()
        backupManager.writeLatestSnapshot(meds, logs).fold(
            onSuccess = {
                markBackedUp()
                BackupOutcome.Success(
                    "Đã sao lưu ${meds.size} thuốc và ${logs.size} lượt uống."
                )
            },
            onFailure = { BackupOutcome.Failure("Sao lưu thất bại: ${it.message}") }
        )
    }

    suspend fun prescriptionCsv(): String = withContext(Dispatchers.IO) {
        MedicationCsv.buildPrescriptionCsv(medicationDao.getAllMedicationsOnce())
    }

    suspend fun historyCsv(): String = withContext(Dispatchers.IO) {
        MedicationCsv.buildHistoryCsv(medicationDao.getAllLogsOnce())
    }

    suspend fun exportPrescription(uri: Uri): BackupOutcome = withContext(Dispatchers.IO) {
        backupManager.exportToUri(uri, prescriptionCsv()).fold(
            onSuccess = { BackupOutcome.Success("Đã xuất đơn thuốc ra file CSV.") },
            onFailure = { BackupOutcome.Failure("Xuất đơn thuốc thất bại: ${it.message}") }
        )
    }

    suspend fun exportHistory(uri: Uri): BackupOutcome = withContext(Dispatchers.IO) {
        backupManager.exportToUri(uri, historyCsv()).fold(
            onSuccess = { BackupOutcome.Success("Đã xuất lịch sử uống thuốc ra file CSV.") },
            onFailure = { BackupOutcome.Failure("Xuất lịch sử thất bại: ${it.message}") }
        )
    }

    /**
     * Restores from the rolling snapshot after a wipe. Existing rows are kept:
     * medications are only re-added when the table is empty, history rows are
     * upserted (the unique index on medicationId+date+session dedupes them).
     */
    suspend fun restoreFromLatestSnapshot(): BackupOutcome = withContext(Dispatchers.IO) {
        runCatching {
            val meds = backupManager.readLatestPrescription()
            val logs = backupManager.readLatestHistory()
            if (meds.isEmpty() && logs.isEmpty()) {
                return@withContext BackupOutcome.Failure("Chưa có bản sao lưu nào để khôi phục.")
            }

            if (medicationDao.getAllMedicationsOnce().isEmpty() && meds.isNotEmpty()) {
                medicationDao.replaceAllMedications(meds)
            }

            // Map old ids to the ids Room just assigned, by medicine name.
            val byName = medicationDao.getAllMedicationsOnce().associateBy { it.name }
            logs.forEach { log ->
                val resolvedId = byName[log.medicationNameSnapshot]?.id ?: log.medicationId
                medicationDao.upsertLog(log.copy(id = 0L, medicationId = resolvedId))
            }
            BackupOutcome.Success(
                "Đã khôi phục ${meds.size} thuốc và ${logs.size} lượt uống."
            ) as BackupOutcome
        }.getOrElse { BackupOutcome.Failure("Khôi phục thất bại: ${it.message}") }
    }

    suspend fun importPrescriptionFromUri(uri: Uri): BackupOutcome = withContext(Dispatchers.IO) {
        backupManager.readTextFromUri(uri).fold(
            onSuccess = { text ->
                val meds = MedicationCsv.parsePrescriptionCsv(text)
                if (meds.isEmpty()) {
                    BackupOutcome.Failure("File không có dòng thuốc hợp lệ.")
                } else {
                    medicationDao.replaceAllMedications(meds)
                    BackupOutcome.Success("Đã nhập ${meds.size} thuốc từ file CSV.")
                }
            },
            onFailure = { BackupOutcome.Failure("Không đọc được file: ${it.message}") }
        )
    }

    fun snapshotFiles(): List<File> = backupManager.listSnapshots()

    private fun markBackedUp() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_BACKUP_AT, now).apply()
        _lastBackupAt.value = now
    }

    private companion object {
        const val TAG = "MedBackupRepo"
        const val PREFS_NAME = "medication_backup_prefs"
        const val KEY_LAST_VERSION = "last_backed_up_version"
        const val KEY_LAST_BACKUP_AT = "last_backup_at"
        const val AUTO_BACKUP_DEBOUNCE_MS = 1_500L
    }
}
