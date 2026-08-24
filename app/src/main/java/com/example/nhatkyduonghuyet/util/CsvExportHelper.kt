package com.example.nhatkyduonghuyet.util

import android.content.Context
import android.net.Uri
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object CsvExportHelper {

    private val CSV_HEADERS = listOf(
        "Ngay",
        "Buoi",
        "Loai insulin/thuoc",
        "Lieu (dv/vien)",
        "Gio tiem/uong",
        "Duong huyet truoc (mmol/L)",
        "Duong huyet sau 2 gio (mmol/L)",
        "Trieu chung/Ghi chu"
    )

    /**
     * Escape gia tri CSV: boc trong dau ngoac kep neu chua dau phay hoac xuong dong
     */
    private fun escapeCsv(value: String): String {
        return when {
            value.contains(",") || value.contains("\n") || value.contains("\r") || value.contains("\"") -> {
                "\"" + value.replace("\"", "\"\"") + "\""
            }
            else -> value
        }
    }

    fun exportLogEntriesToCsv(context: Context, uri: Uri, logEntries: List<LogEntry>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // Write headers
                    writer.append(CSV_HEADERS.joinToString(",") { escapeCsv(it) })
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
                        writer.append(row.joinToString(",") { escapeCsv(it) })
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
                    reader.readLine()

                    var line: String? = reader.readLine()
                    while (line != null) {
                        if (line.isNotBlank()) {
                            val parts = parseCsvLine(line)
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

    /**
     * Parse dong CSV co ho tro gia tri boc trong dau ngoac kep
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && !inQuotes -> {
                    inQuotes = true
                }
                char == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}