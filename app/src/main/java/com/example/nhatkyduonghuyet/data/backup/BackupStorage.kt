package com.example.nhatkyduonghuyet.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File-level concerns for backups: where snapshots live, how they are written
 * safely, and how many dated copies to keep.
 *
 * Kept separate from [BackupRepository] so the repository deals only with
 * "what to back up" and this class only with "how to put bytes on disk".
 */
@Singleton
class BackupStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val root: File
        get() = File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /** The rolling snapshot: one file per dataset, always current. */
    private fun rollingFile(part: BackupPart) = File(root, "${part.fileStem}_latest.csv")

    fun writeRolling(snapshot: BackupSnapshot): Result<Unit> = runCatching {
        BackupPart.entries.forEach { part ->
            writeAtomically(rollingFile(part), BackupCsv.encode(part, snapshot))
        }
    }.onFailure { Log.e(TAG, "Khong ghi duoc ban sao luu", it) }

    fun readRolling(): BackupSnapshot {
        var result = BackupSnapshot()
        BackupPart.entries.forEach { part ->
            val file = rollingFile(part)
            if (!file.exists()) return@forEach
            runCatching { BackupCsv.decode(part, file.readText()) }
                .onSuccess { piece ->
                    result = BackupSnapshot(
                        logEntries = result.logEntries + piece.logEntries,
                        medications = result.medications + piece.medications,
                        medicationLogs = result.medicationLogs + piece.medicationLogs
                    )
                }
                .onFailure { Log.e(TAG, "Khong doc duoc ${file.name}", it) }
        }
        return result
    }

    /** A dated copy of every dataset, tagged with the app version. */
    fun writeVersioned(versionName: String, snapshot: BackupSnapshot): Result<List<File>> =
        runCatching {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val safeVersion = versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val written = BackupPart.entries.map { part ->
                File(root, "${part.fileStem}_v${safeVersion}_$stamp.csv").also {
                    writeAtomically(it, BackupCsv.encode(part, snapshot))
                }
            }
            prune()
            written
        }.onFailure { Log.e(TAG, "Khong ghi duoc ban sao luu theo phien ban", it) }

    fun listSnapshots(): List<File> =
        root.listFiles { f -> f.isFile && f.extension.equals("csv", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun writeToUri(uri: Uri, content: String): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error("Không mở được file đích")
    }

    fun readFromUri(uri: Uri): Result<String> = runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: error("Không đọc được file")
    }

    /**
     * Write to a temp file and rename, so a process death mid-write cannot
     * leave a truncated snapshot where a good one used to be.
     */
    private fun writeAtomically(target: File, content: String) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        if (target.exists() && !target.delete()) {
            Log.w(TAG, "Khong xoa duoc file cu ${target.name}")
        }
        if (!tmp.renameTo(target)) {
            target.writeText(content, Charsets.UTF_8)
            tmp.delete()
        }
    }

    /** Keeps the newest dated copies and drops the rest. */
    private fun prune() {
        val versioned = root.listFiles { f ->
            f.isFile && f.name.contains("_v") && f.extension.equals("csv", true)
        }?.sortedByDescending { it.lastModified() } ?: return
        versioned.drop(MAX_VERSIONED_FILES).forEach { it.delete() }
    }

    private companion object {
        const val TAG = "BackupStorage"
        const val DIR_NAME = "backups"
        // 3 files per version -> ~10 app versions of history.
        const val MAX_VERSIONED_FILES = 30
    }
}
