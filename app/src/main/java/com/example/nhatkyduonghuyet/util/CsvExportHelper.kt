package com.example.nhatkyduonghuyet.util

import android.content.Context
import android.net.Uri
import com.example.nhatkyduonghuyet.data.LogEntry
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
}
