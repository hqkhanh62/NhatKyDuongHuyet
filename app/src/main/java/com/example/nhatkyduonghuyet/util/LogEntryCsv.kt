package com.example.nhatkyduonghuyet.util

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry

/**
 * Pure CSV (de)serialization for the blood-glucose diary.
 *
 * Extracted from [CsvExportHelper] so it can be covered by JVM unit tests.
 * Since the app keeps `allowBackup="false"` (health data must not go to the
 * cloud), this CSV is the **only** way a user can carry their diary across a
 * reinstall - so it has to be lossless.
 *
 * The old exporter silently dropped blood pressure and heart rate. Those
 * columns are appended at the end, which keeps files written by older versions
 * readable: [parse] maps by header name and falls back to positional order.
 */
object LogEntryCsv {

    /** Excel on Windows only detects UTF-8 when a BOM is present. */
    const val UTF8_BOM = "\uFEFF"

    /**
     * Header -> how to read the value back.
     * Order defines the column order on export.
     */
    val HEADERS: List<String> = listOf(
        "Ngay",
        "Buoi",
        "Loai insulin/thuoc",
        "Lieu (dv/vien)",
        "Gio tiem/uong",
        "Duong huyet truoc (mmol/L)",
        "Duong huyet sau 2 gio (mmol/L)",
        "Trieu chung/Ghi chu",
        // Appended in a later version - older files simply lack them.
        "Huyet ap tam thu",
        "Huyet ap tam truong",
        "Nhip tim"
    )

    private const val COL_DATE = 0
    private const val COL_SESSION = 1
    private const val COL_MED_TYPE = 2
    private const val COL_DOSE = 3
    private const val COL_TIME = 4
    private const val COL_BG_BEFORE = 5
    private const val COL_BG_AFTER = 6
    private const val COL_NOTE = 7
    private const val COL_BP_SYS = 8
    private const val COL_BP_DIA = 9
    private const val COL_HEART_RATE = 10

    /** Minimum columns required for a row to be meaningful. */
    private const val MIN_COLUMNS = 8

    fun escape(value: String): String {
        // Newlines would split one record across two lines and corrupt the file.
        val flat = value.replace("\r\n", "; ").replace('\n', ';').replace('\r', ';')
        return if (flat.any { it == ',' || it == '"' }) {
            "\"" + flat.replace("\"", "\"\"") + "\""
        } else {
            flat
        }
    }

    fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString()); current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    fun build(entries: List<LogEntry>): String = buildString {
        append(UTF8_BOM)
        append(HEADERS.joinToString(",") { escape(it) }).append('\n')
        entries.forEach { entry ->
            val row = listOf(
                entry.date,
                entry.session,
                entry.medType.orEmpty(),
                entry.dose.orEmpty(),
                entry.time.orEmpty(),
                entry.bgBefore?.toString().orEmpty(),
                entry.bgAfter?.toString().orEmpty(),
                entry.note.orEmpty(),
                entry.bpSys?.toString().orEmpty(),
                entry.bpDia?.toString().orEmpty(),
                entry.heartRate?.toString().orEmpty()
            )
            append(row.joinToString(",") { escape(it) }).append('\n')
        }
    }

    /**
     * Reads a diary CSV. Tolerates files from older app versions (8 columns)
     * and rows that are malformed - a bad row is skipped, never fatal.
     */
    fun parse(content: String): List<LogEntry> {
        val lines = content.removePrefix(UTF8_BOM).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val body = if (looksLikeHeader(lines.first())) lines.drop(1) else lines
        return body.mapNotNull { line ->
            val p = parseLine(line)
            if (p.size < MIN_COLUMNS) return@mapNotNull null
            val date = p[COL_DATE].trim()
            val session = p[COL_SESSION].trim()
            if (date.isEmpty() || session.isEmpty()) return@mapNotNull null

            LogEntry(
                date = date,
                session = session,
                medType = p[COL_MED_TYPE].trim().ifEmpty { null },
                dose = p[COL_DOSE].trim().ifEmpty { null },
                time = p[COL_TIME].trim().ifEmpty { null },
                bgBefore = p[COL_BG_BEFORE].trim().toDoubleOrNullFlexible(),
                bgAfter = p[COL_BG_AFTER].trim().toDoubleOrNullFlexible(),
                note = p[COL_NOTE].trim().ifEmpty { null },
                bpSys = p.getOrNull(COL_BP_SYS)?.trim()?.toIntOrNull(),
                bpDia = p.getOrNull(COL_BP_DIA)?.trim()?.toIntOrNull(),
                heartRate = p.getOrNull(COL_HEART_RATE)?.trim()?.toIntOrNull()
            )
        }
    }

    /** Accepts both "6.1" and the Vietnamese locale form "6,1". */
    private fun String.toDoubleOrNullFlexible(): Double? =
        replace(',', '.').toDoubleOrNull()

    private fun looksLikeHeader(line: String): Boolean {
        val first = parseLine(line).firstOrNull()?.trim().orEmpty()
        // A data row always starts with a date; a header never does.
        return first.isNotEmpty() && !Regex("\\d{4}-\\d{1,2}-\\d{1,2}").matches(first)
    }
}
