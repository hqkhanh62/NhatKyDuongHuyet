package com.example.nhatkyduonghuyet.util

import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure CSV (de)serialization for the prescription and the intake history.
 *
 * Kept free of Android types so it can be covered by fast JVM unit tests: a
 * backup format that silently corrupts data is worse than no backup at all.
 */
object MedicationCsv {

    /** Excel on Windows only detects UTF-8 with a BOM; Vietnamese needs it. */
    const val UTF8_BOM = "\uFEFF"

    val PRESCRIPTION_HEADERS = listOf(
        "ID",
        "Ten thuoc",
        "Ham luong",
        "Lieu dung",
        "Thoi diem"
    )

    val HISTORY_HEADERS = listOf(
        "Ngay",
        "Buoi",
        "Gio uong",
        "Ten thuoc",
        "Ham luong",
        "So luong",
        "MedicationID",
        "Timestamp"
    )

    /** Vietnamese labels for the stored session keys. */
    private val SESSION_LABELS = linkedMapOf(
        "MORNING" to "Sang",
        "NOON" to "Trua",
        "AFTERNOON" to "Chieu",
        "EVENING" to "Toi",
        "BEDTIME" to "Truoc khi ngu"
    )

    fun sessionLabel(session: String): String =
        SESSION_LABELS[session.uppercase(Locale.US)] ?: session

    private fun sessionKey(label: String): String {
        val trimmed = label.trim()
        SESSION_LABELS.forEach { (key, value) ->
            if (value.equals(trimmed, ignoreCase = true) || key.equals(trimmed, ignoreCase = true)) {
                return key
            }
        }
        return trimmed.uppercase(Locale.US)
    }

    /**
     * Newlines inside a field would break the line-based reader, so they are
     * flattened to "; " on export. Everything else is quoted per RFC4180.
     */
    fun escape(value: String): String {
        val flat = value.replace("\r\n", "; ").replace('\n', ';').replace('\r', ';')
        return if (flat.any { it == ',' || it == '"' }) {
            "\"" + flat.replace("\"", "\"\"") + "\""
        } else {
            flat
        }
    }

    /** RFC4180-ish line parser that understands quoted fields and "" escapes. */
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

    private fun row(values: List<String>): String = values.joinToString(",") { escape(it) }

    // ---------------------------------------------------------------- export

    fun buildPrescriptionCsv(medications: List<Medication>): String = buildString {
        append(UTF8_BOM)
        append(row(PRESCRIPTION_HEADERS)).append('\n')
        medications.forEach { med ->
            append(
                row(
                    listOf(
                        med.id.toString(),
                        med.name,
                        med.dosage,
                        med.instruction,
                        med.timing
                    )
                )
            ).append('\n')
        }
    }

    fun buildHistoryCsv(logs: List<MedicationLog>): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        return buildString {
            append(UTF8_BOM)
            append(row(HISTORY_HEADERS)).append('\n')
            logs.forEach { log ->
                append(
                    row(
                        listOf(
                            log.date,
                            sessionLabel(log.session),
                            timeFormat.format(Date(log.timestamp)),
                            log.medicationNameSnapshot,
                            log.dosageSnapshot,
                            trimFloat(log.amountTaken),
                            log.medicationId.toString(),
                            log.timestamp.toString()
                        )
                    )
                ).append('\n')
            }
        }
    }

    private fun trimFloat(value: Float): String =
        if (value % 1f == 0f) value.toInt().toString() else value.toString()

    // ---------------------------------------------------------------- import

    /**
     * Reads back a prescription CSV produced by [buildPrescriptionCsv].
     *
     * Also accepts the legacy layout used by the old import screen, where the
     * columns were shifted by one (`STT, Ten, Ham luong, Lieu, Thoi diem`), and
     * rows are skipped rather than rejected wholesale when malformed.
     */
    fun parsePrescriptionCsv(content: String): List<Medication> {
        val lines = content.removePrefix(UTF8_BOM).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val body = if (looksLikeHeader(lines.first())) lines.drop(1) else lines
        return body.mapNotNull { line ->
            val parts = parseLine(line)
            if (parts.size < 3) return@mapNotNull null
            val name = parts[1].trim()
            if (name.isEmpty()) return@mapNotNull null
            Medication(
                id = 0L, // let Room assign, avoids clashing with existing rows
                name = name,
                dosage = parts.getOrNull(2)?.trim().orEmpty(),
                instruction = parts.getOrNull(3)?.trim().orEmpty(),
                timing = parts.getOrNull(4)?.trim().orEmpty()
            )
        }
    }

    fun parseHistoryCsv(content: String): List<MedicationLog> {
        val lines = content.removePrefix(UTF8_BOM).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val body = if (looksLikeHeader(lines.first())) lines.drop(1) else lines

        return body.mapNotNull { line ->
            val parts = parseLine(line)
            if (parts.size < 6) return@mapNotNull null
            val date = parts[0].trim()
            if (date.isEmpty()) return@mapNotNull null
            MedicationLog(
                id = 0L,
                medicationId = parts.getOrNull(6)?.trim()?.toLongOrNull() ?: 0L,
                medicationNameSnapshot = parts.getOrNull(3)?.trim().orEmpty(),
                dosageSnapshot = parts.getOrNull(4)?.trim().orEmpty(),
                timestamp = parts.getOrNull(7)?.trim()?.toLongOrNull() ?: 0L,
                date = date,
                session = sessionKey(parts.getOrNull(1).orEmpty()),
                amountTaken = parts.getOrNull(5)?.trim()?.replace(',', '.')?.toFloatOrNull() ?: 1f
            )
        }
    }

    private fun looksLikeHeader(line: String): Boolean {
        val first = parseLine(line).firstOrNull()?.trim()?.lowercase(Locale.US).orEmpty()
        return first.isNotEmpty() && first.toLongOrNull() == null &&
            !Regex("\\d{4}-\\d{2}-\\d{2}").matches(first)
    }
}
