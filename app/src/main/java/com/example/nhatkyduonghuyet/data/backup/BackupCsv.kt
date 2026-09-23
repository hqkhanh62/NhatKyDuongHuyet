package com.example.nhatkyduonghuyet.data.backup

import com.example.nhatkyduonghuyet.util.LogEntryCsv
import com.example.nhatkyduonghuyet.util.MedicationCsv

/**
 * Single entry point for turning a [BackupSnapshot] into CSV text and back.
 *
 * The per-table encoders ([LogEntryCsv], [MedicationCsv]) already exist and are
 * unit tested; this object only routes between them so that callers never have
 * to remember which encoder belongs to which file. Adding a table means adding
 * one [BackupPart] and one branch here - the UI and the backup scheduler do not
 * change at all.
 */
object BackupCsv {

    fun encode(part: BackupPart, snapshot: BackupSnapshot): String = when (part) {
        BackupPart.DIARY -> LogEntryCsv.build(snapshot.logEntries)
        BackupPart.PRESCRIPTION -> MedicationCsv.buildPrescriptionCsv(snapshot.medications)
        BackupPart.MEDICATION_HISTORY -> MedicationCsv.buildHistoryCsv(snapshot.medicationLogs)
    }

    /** Decodes one file into the matching slice of a snapshot. */
    fun decode(part: BackupPart, content: String): BackupSnapshot = when (part) {
        BackupPart.DIARY -> BackupSnapshot(logEntries = LogEntryCsv.parse(content))
        BackupPart.PRESCRIPTION ->
            BackupSnapshot(medications = MedicationCsv.parsePrescriptionCsv(content))
        BackupPart.MEDICATION_HISTORY ->
            BackupSnapshot(medicationLogs = MedicationCsv.parseHistoryCsv(content))
    }

    /**
     * Guesses which dataset a file holds, so "Khôi phục" can accept a file the
     * user picked without asking them which of the three it is.
     *
     * Detection is by header signature and falls back to the file name.
     */
    fun detectPart(fileName: String?, content: String): BackupPart? {
        val firstLine = content
            .removePrefix(LogEntryCsv.UTF8_BOM)
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.lowercase()
            .orEmpty()

        // Header-based detection is the most reliable: it survives renaming.
        when {
            // Diary is the only file with a blood-glucose column.
            firstLine.contains("duong huyet truoc") -> return BackupPart.DIARY
            // History has a timestamp column; the prescription does not.
            firstLine.contains("timestamp") || firstLine.contains("gio uong") ->
                return BackupPart.MEDICATION_HISTORY
            firstLine.contains("ten thuoc") && firstLine.contains("lieu dung") ->
                return BackupPart.PRESCRIPTION
        }

        val name = fileName?.lowercase().orEmpty()
        return BackupPart.entries.firstOrNull { name.contains(it.fileStem) }
    }
}
