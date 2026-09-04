package com.example.nhatkyduonghuyet.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps a CSV copy of the prescription and the intake history on disk.
 *
 * Why this exists: the Room database is built with `fallbackToDestructiveMigration()`,
 * so bumping the schema during an app update wipes every table. The CSV snapshots
 * live in the app's *files* directory (not the database), survive updates, and
 * can be re-imported after a wipe.
 *
 * Two kinds of snapshot are written:
 *  - `latest`  : overwritten on every data change (the restore source)
 *  - versioned : one dated copy per app version, kept as history
 */
@Singleton
class MedicationBackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val backupDir: File
        get() = File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    val latestPrescriptionFile: File get() = File(backupDir, "don_thuoc_latest.csv")
    val latestHistoryFile: File get() = File(backupDir, "lich_su_uong_thuoc_latest.csv")

    /**
     * Writes the rolling snapshot. Called whenever the prescription or the
     * intake history changes.
     */
    fun writeLatestSnapshot(
        medications: List<Medication>,
        logs: List<MedicationLog>
    ): Result<Unit> = runCatching {
        writeAtomically(latestPrescriptionFile, MedicationCsv.buildPrescriptionCsv(medications))
        writeAtomically(latestHistoryFile, MedicationCsv.buildHistoryCsv(logs))
    }.onFailure { Log.e(TAG, "Khong ghi duoc ban sao luu CSV", it) }

    /**
     * Writes an extra dated copy tagged with the app version. Used once per
     * app update so a destructive migration always leaves a trail behind.
     */
    fun writeVersionedSnapshot(
        versionName: String,
        medications: List<Medication>,
        logs: List<MedicationLog>
    ): Result<List<File>> = runCatching {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val safeVersion = versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val prescription = File(backupDir, "don_thuoc_v${safeVersion}_$stamp.csv")
        val history = File(backupDir, "lich_su_uong_thuoc_v${safeVersion}_$stamp.csv")
        writeAtomically(prescription, MedicationCsv.buildPrescriptionCsv(medications))
        writeAtomically(history, MedicationCsv.buildHistoryCsv(logs))
        pruneOldSnapshots()
        listOf(prescription, history)
    }.onFailure { Log.e(TAG, "Khong ghi duoc ban sao luu theo phien ban", it) }

    /** All snapshots on disk, newest first. */
    fun listSnapshots(): List<File> =
        backupDir.listFiles { f -> f.isFile && f.extension.equals("csv", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun readLatestPrescription(): List<Medication> =
        runCatching {
            if (latestPrescriptionFile.exists()) {
                MedicationCsv.parsePrescriptionCsv(latestPrescriptionFile.readText())
            } else {
                emptyList()
            }
        }.getOrElse { emptyList() }

    fun readLatestHistory(): List<MedicationLog> =
        runCatching {
            if (latestHistoryFile.exists()) {
                MedicationCsv.parseHistoryCsv(latestHistoryFile.readText())
            } else {
                emptyList()
            }
        }.getOrElse { emptyList() }

    /** Copies CSV content to a user-chosen location (SAF). */
    fun exportToUri(uri: Uri, content: String): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error("Khong mo duoc file dich")
    }.onFailure { Log.e(TAG, "Xuat CSV that bai", it) }

    fun readTextFromUri(uri: Uri): Result<String> = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("Khong doc duoc file")
    }

    /**
     * Write to a temp file then rename, so an interrupted write (app killed
     * mid-backup) cannot leave a truncated snapshot behind.
     */
    private fun writeAtomically(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content, Charsets.UTF_8)
        if (target.exists() && !target.delete()) {
            Log.w(TAG, "Khong xoa duoc file cu ${target.name}")
        }
        if (!tmp.renameTo(target)) {
            // Fallback for filesystems that refuse the rename.
            target.writeText(content, Charsets.UTF_8)
            tmp.delete()
        }
    }

    /** Keeps the newest [MAX_VERSIONED_SNAPSHOTS] dated copies. */
    private fun pruneOldSnapshots() {
        val versioned = backupDir.listFiles { f ->
            f.isFile && f.name.contains("_v") && f.extension.equals("csv", true)
        }?.sortedByDescending { it.lastModified() } ?: return
        versioned.drop(MAX_VERSIONED_SNAPSHOTS).forEach { it.delete() }
    }

    private companion object {
        const val TAG = "MedBackup"
        const val DIR_NAME = "medication_backups"
        // 2 files per app version -> ~10 versions of history retained.
        const val MAX_VERSIONED_SNAPSHOTS = 20
    }
}
