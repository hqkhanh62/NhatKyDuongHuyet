package com.example.nhatkyduonghuyet.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry

/**
 * Thin Android wrapper around [LogEntryCsv].
 *
 * All parsing/formatting lives in [LogEntryCsv] so it can be unit tested on the
 * JVM; this object only deals with content URIs.
 */
object CsvExportHelper {

    fun exportLogEntriesToCsv(context: Context, uri: Uri, logEntries: List<LogEntry>): Boolean =
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(LogEntryCsv.build(logEntries).toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Khong mo duoc file dich")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Xuat nhat ky CSV that bai", e)
            false
        }

    fun importCsv(context: Context, uri: Uri): List<LogEntry> =
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                LogEntryCsv.parse(input.readBytes().toString(Charsets.UTF_8))
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Nhap nhat ky CSV that bai", e)
            emptyList()
        }

    private const val TAG = "CsvExportHelper"
}
