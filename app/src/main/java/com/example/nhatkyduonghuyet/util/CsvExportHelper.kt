package com.example.nhatkyduonghuyet.util

import android.content.Context
import android.net.Uri
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object CsvExportHelper {

    private val CSV_HEADERS = listOf(
        "Ngày",
        "Buổi",
        "Loại insulin/thuốc",
        "Liều (đv/viên)",
        "Giờ tiêm/uống",
        "Đường huyết trước (mmol/L)",
        "Đường huyết sau 2 giờ (mmol/L)",
        "Triệu chứng/Ghi chú"
    )

    fun exportLogEntriesToCsv(context: Context, uri: Uri, logEntries: List<LogEntry>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // Write headers
                    writer.append(CSV_HEADERS.joinToString(","))
                    writer.append("\n")

                    // Write data
                    logEntries.forEach { entry ->
                        val row = listOf(
                            entry.date,
                            entry.session,
                            entry.medType ?: "",
                            entry.dose ?: "",
                            entry.time ?: "",
                            entry.bgBefore?.toString() ?: "",
                            entry.bgAfter?.toString() ?: "",
                            entry.note ?: ""
                        )
                        writer.append(row.joinToString(","))
                        writer.append("\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importCsv(context: Context, uri: Uri): List<LogEntry> {
        val importedEntries = mutableListOf<LogEntry>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header
                    val header = reader.readLine()
                    
                    var line: String? = reader.readLine()
                    while (line != null) {
                        if (line.isNotBlank()) {
                            val parts = line.split(",")
                            if (parts.size >= 8) {
                                val entry = LogEntry(
                                    date = parts[0].trim(),
                                    session = parts[1].trim(),
                                    medType = parts[2].trim().ifEmpty { null },
                                    dose = parts[3].trim().ifEmpty { null },
                                    time = parts[4].trim().ifEmpty { null },
                                    bgBefore = parts[5].trim().toDoubleOrNull(),
                                    bgAfter = parts[6].trim().toDoubleOrNull(),
                                    note = parts[7].trim().ifEmpty { null }
                                )
                                importedEntries.add(entry)
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return importedEntries
    }
}
