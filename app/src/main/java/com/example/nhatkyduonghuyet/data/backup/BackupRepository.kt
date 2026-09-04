package com.example.nhatkyduonghuyet.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.dao.MedicationDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One place responsible for keeping the user's data recoverable.
 *
 * Design notes
 * ------------
 * The app ships with `allowBackup="false"` (health data must not leave the
 * device), so Android's cloud backup will never help. That makes local
 * snapshots plus user-driven export the only recovery path, and it has to cover
 * **every** table - the previous implementation backed up the medication tables
 * but silently ignored the blood-glucose diary.
 *
 * Three layers, each covering a failure the others cannot:
 *  1. rolling snapshot - rewritten on every data change, survives app updates
 *  2. versioned snapshot - one dated copy per app version, survives a bad update
 *  3. user export - the only thing that survives an uninstall
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logEntryDao: LogEntryDao,
    private val medicationDao: MedicationDao,
    private val storage: BackupStorage
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _lastBackupAt = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP_AT, 0L))
    val lastBackupAt: Flow<Long> = _lastBackupAt.asStateFlow()

    private val _lastExportAt = MutableStateFlow(prefs.getLong(KEY_LAST_EXPORT_AT, 0L))

    /**
     * Days since the user last exported to a location that survives an
     * uninstall. `null` means "never". Drives the reminder banner.
     */
    val daysSinceExport: Flow<Long?> = _lastExportAt.map(::computeDaysSince)

    private fun computeDaysSince(at: Long): Long? =
        if (at <= 0L) null else (System.currentTimeMillis() - at) / DAY_MS

    /** Reads every table into one value. */
    suspend fun snapshot(): BackupSnapshot = withContext(Dispatchers.IO) {
        BackupSnapshot(
            logEntries = logEntryDao.getAllLogEntriesOnce(),
            medications = medicationDao.getAllMedicationsOnce(),
            medicationLogs = medicationDao.getAllLogsOnce()
        )
    }

    // ------------------------------------------------------------ layer 1 & 2

    /**
     * Mirrors every table to disk whenever anything changes. Debounced so that
     * ticking several checkboxes in a row writes once.
     */
    @OptIn(FlowPreview::class)
    fun startAutoBackup(scope: CoroutineScope) {
        combine(
            logEntryDao.getAllLogEntries(),
            medicationDao.getAllMedications(),
            medicationDao.observeAllLogs()
        ) { diary, meds, medLogs -> BackupSnapshot(diary, meds, medLogs) }
            .debounce(AUTO_BACKUP_DEBOUNCE_MS)
            .onEach { snapshot ->
                if (snapshot.isEmpty) return@onEach
                withContext(Dispatchers.IO) {
                    if (storage.writeRolling(snapshot).isSuccess) markBackedUp()
                }
            }
            .launchIn(scope)
    }

    /**
     * Writes a dated copy the first time the app runs after an update. Room's
     * migrations now preserve data, but a snapshot taken *before* the new code
     * touches the database is still the cheapest insurance against a bad
     * migration shipping in a future release.
     */
    suspend fun backupIfAppUpdated(currentVersion: String) = withContext(Dispatchers.IO) {
        if (prefs.getString(KEY_LAST_VERSION, null) == currentVersion) return@withContext
        val snapshot = snapshot()
        if (!snapshot.isEmpty) {
            val written = storage.writeVersioned(currentVersion, snapshot)
            if (written.isSuccess) {
                Log.i(TAG, "Da sao luu truoc khi chay phien ban $currentVersion")
                markBackedUp()
            }
        }
        prefs.edit().putString(KEY_LAST_VERSION, currentVersion).apply()
    }

    suspend fun backupNow(): BackupResult = withContext(Dispatchers.IO) {
        val snapshot = snapshot()
        if (snapshot.isEmpty) {
            return@withContext BackupResult.Failure("Chưa có dữ liệu nào để sao lưu.")
        }
        val written = storage.writeRolling(snapshot)
        if (written.isSuccess) {
            markBackedUp()
            BackupResult.Success("Đã sao lưu ${snapshot.totalRows} dòng dữ liệu.")
        } else {
            BackupResult.Failure(
                "Sao lưu thất bại: ${written.exceptionOrNull()?.message}"
            )
        }
    }

    // ---------------------------------------------------------------- layer 3

    fun suggestedFileName(part: BackupPart): String =
        "${part.fileStem}_${timestamp()}.csv"

    /** Exports one dataset to a user-chosen location. */
    suspend fun export(part: BackupPart, uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val snapshot = snapshot()
        val content = BackupCsv.encode(part, snapshot)
        val written = storage.writeToUri(uri, content)
        if (written.isSuccess) {
            markExported()
            BackupResult.Success("Đã xuất ${part.label.lowercase()}.")
        } else {
            BackupResult.Failure("Xuất thất bại: ${written.exceptionOrNull()?.message}")
        }
    }

    /**
     * Restores from a file the user picked. The dataset is detected from the
     * file's header, so the user does not have to tell us what they chose.
     */
    suspend fun restoreFromUri(uri: Uri, displayName: String?): BackupResult =
        withContext(Dispatchers.IO) {
            val read = storage.readFromUri(uri)
            val content = read.getOrNull()
                ?: return@withContext BackupResult.Failure(
                    "Không đọc được file: ${read.exceptionOrNull()?.message}"
                )
            val part = BackupCsv.detectPart(displayName, content)
                ?: return@withContext BackupResult.Failure(
                    "Không nhận dạng được file. Hãy chọn file CSV do chính app xuất ra."
                )
            val report = applySnapshot(BackupCsv.decode(part, content))
            if (report.touchedAnything) {
                BackupResult.Success("${part.label}: ${report.describe()}")
            } else {
                BackupResult.Failure(report.describe())
            }
        }

    /** Restores every dataset from the rolling snapshot on disk. */
    suspend fun restoreFromRollingSnapshot(): BackupResult = withContext(Dispatchers.IO) {
        val stored = storage.readRolling()
        if (stored.isEmpty) {
            return@withContext BackupResult.Failure("Chưa có bản sao lưu nào để khôi phục.")
        }
        val report = applySnapshot(stored)
        if (report.touchedAnything) BackupResult.Success(report.describe())
        else BackupResult.Failure(report.describe())
    }

    // ---------------------------------------------------------------- merging

    /**
     * Merges a snapshot into the database.
     *
     * Restore is **additive and idempotent**: importing the same file twice must
     * not create duplicates, and it must never delete something the user typed
     * after the backup was taken. Rows are matched on their natural key
     * (diary: date+session, medication: name) rather than on autogenerated ids,
     * which are meaningless across a wipe.
     */
    suspend fun applySnapshot(incoming: BackupSnapshot): RestoreReport =
        withContext(Dispatchers.IO) {
            var diaryAdded = 0
            var diaryUpdated = 0
            var medsAdded = 0
            var medLogsAdded = 0
            var skipped = 0

            incoming.logEntries.forEach { entry ->
                if (entry.date.isBlank() || entry.session.isBlank()) {
                    skipped++
                    return@forEach
                }
                runCatching {
                    val existing = logEntryDao.findByDateAndSession(entry.date, entry.session)
                    if (existing == null) {
                        logEntryDao.upsert(entry.copy(id = 0L))
                        diaryAdded++
                    } else {
                        // Keep the row id so we update instead of duplicating.
                        logEntryDao.upsert(entry.copy(id = existing.id))
                        diaryUpdated++
                    }
                }.onFailure { skipped++ }
            }

            if (incoming.medications.isNotEmpty()) {
                val existingByName = medicationDao.getAllMedicationsOnce()
                    .associateBy { it.name }
                incoming.medications.forEach { med ->
                    if (med.name.isBlank()) {
                        skipped++
                        return@forEach
                    }
                    if (existingByName[med.name] == null) {
                        runCatching { medicationDao.insertMedication(med.copy(id = 0L)) }
                            .onSuccess { medsAdded++ }
                            .onFailure { skipped++ }
                    }
                }
            }

            if (incoming.medicationLogs.isNotEmpty()) {
                // Ids from the backup are stale after a wipe; re-resolve by name.
                val byName = medicationDao.getAllMedicationsOnce().associateBy { it.name }
                incoming.medicationLogs.forEach { log ->
                    val resolvedId = byName[log.medicationNameSnapshot]?.id ?: log.medicationId
                    runCatching {
                        // The unique index on (medicationId, date, session)
                        // makes this a no-op for rows already present.
                        medicationDao.upsertLog(log.copy(id = 0L, medicationId = resolvedId))
                    }.onSuccess { medLogsAdded++ }.onFailure { skipped++ }
                }
            }

            RestoreReport(diaryAdded, diaryUpdated, medsAdded, medLogsAdded, skipped)
        }

    fun snapshotFiles(): List<File> = storage.listSnapshots()

    private fun markBackedUp() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_BACKUP_AT, now).apply()
        _lastBackupAt.value = now
    }

    private fun markExported() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_EXPORT_AT, now).apply()
        _lastExportAt.value = now
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

    private companion object {
        const val TAG = "BackupRepository"
        const val PREFS_NAME = "backup_prefs"
        const val KEY_LAST_VERSION = "last_backed_up_version"
        const val KEY_LAST_BACKUP_AT = "last_backup_at"
        const val KEY_LAST_EXPORT_AT = "last_export_at"
        const val AUTO_BACKUP_DEBOUNCE_MS = 1_500L
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
