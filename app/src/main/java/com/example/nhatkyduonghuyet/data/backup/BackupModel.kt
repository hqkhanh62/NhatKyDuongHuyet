package com.example.nhatkyduonghuyet.data.backup

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog

/**
 * Everything the app owns, in one value.
 *
 * The previous design had three unrelated export paths (diary CSV, prescription
 * CSV, medication-history CSV) spread over two screens, and the *automatic*
 * backup covered only the medication tables - the blood-glucose diary, which is
 * the whole point of the app, was never snapshotted. A single aggregate makes it
 * impossible to add a table and silently forget to back it up.
 */
data class BackupSnapshot(
    val logEntries: List<LogEntry> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val medicationLogs: List<MedicationLog> = emptyList()
) {
    val isEmpty: Boolean
        get() = logEntries.isEmpty() && medications.isEmpty() && medicationLogs.isEmpty()

    val totalRows: Int
        get() = logEntries.size + medications.size + medicationLogs.size
}

/** The three datasets a backup is made of. */
enum class BackupPart(
    /** Stable file stem; also the entry name inside the archive. */
    val fileStem: String,
    /** Vietnamese label for the UI. */
    val label: String
) {
    DIARY("nhat_ky_duong_huyet", "Nhật ký đường huyết"),
    PRESCRIPTION("don_thuoc", "Đơn thuốc"),
    MEDICATION_HISTORY("lich_su_uong_thuoc", "Lịch sử uống thuốc");

    val fileName: String get() = "$fileStem.csv"
}

/**
 * What a restore actually changed. Reported to the user instead of a vague
 * "done", because silently importing 0 rows is indistinguishable from success.
 */
data class RestoreReport(
    val diaryAdded: Int = 0,
    val diaryUpdated: Int = 0,
    val medicationsAdded: Int = 0,
    val medicationLogsAdded: Int = 0,
    val skipped: Int = 0
) {
    val touchedAnything: Boolean
        get() = diaryAdded + diaryUpdated + medicationsAdded + medicationLogsAdded > 0

    fun describe(): String = buildString {
        if (!touchedAnything) {
            append("Không có dữ liệu nào được khôi phục.")
            if (skipped > 0) append(" Bỏ qua $skipped dòng lỗi.")
            return@buildString
        }
        val parts = mutableListOf<String>()
        if (diaryAdded > 0) parts += "$diaryAdded mục nhật ký mới"
        if (diaryUpdated > 0) parts += "$diaryUpdated mục nhật ký cập nhật"
        if (medicationsAdded > 0) parts += "$medicationsAdded thuốc"
        if (medicationLogsAdded > 0) parts += "$medicationLogsAdded lượt uống"
        append("Đã khôi phục: ").append(parts.joinToString(", ")).append('.')
        if (skipped > 0) append(" Bỏ qua $skipped dòng lỗi.")
    }
}

/** Outcome of any backup/export/restore action. */
sealed interface BackupResult {
    data class Success(val message: String) : BackupResult
    data class Failure(val message: String) : BackupResult
}
