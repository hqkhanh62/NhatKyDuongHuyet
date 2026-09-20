package com.example.nhatkyduonghuyet.scan

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.GlucosePolicy
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Where the diary date came from: the meter screen or the system clock. */
enum class ScanDateSource { METER, SYSTEM }

/** Where the diary time came from: the meter screen or the system clock. */
enum class ScanTimeSource { METER, SYSTEM }

/**
 * A scan ready for user review: value already auto-cleaned, date/time
 * resolved (meter-first, system fallback), session classified and the
 * instant risk + provisional HbA1c pre-computed for the Real-time AI Loop.
 */
data class ScanImportDraft(
    val value: Float,
    /** Resolved diary date (`yyyy-MM-dd`). */
    val date: String,
    /** Resolved diary time (`HH:mm`). */
    val time: String,
    /** Auto-classified session: Sáng / Trưa / Chiều / Tối. */
    val session: String,
    val dateSource: ScanDateSource,
    val timeSource: ScanTimeSource,
    val risk: GlucoseRisk,
    /** Provisional HbA1c estimated from this single reading. */
    val hba1cProvisional: Double,
    val rawText: String = "",
    val confidence: Float = 0.8f,
    val scannerSource: String = "ML_KIT"
)

sealed interface ScanSaveResult {
    data class Saved(
        val entry: LogEntry,
        val draft: ScanImportDraft,
        val isUpdate: Boolean
    ) : ScanSaveResult

    data class Duplicate(
        val entry: LogEntry,
        val draft: ScanImportDraft
    ) : ScanSaveResult

    data class Rejected(val reason: String) : ScanSaveResult
}

/**
 * Auto Import Pipeline (Pro AI):
 *
 * 1. **Auto Clean** — rejects OCR noise outside 2.0–30.0 mmol/L.
 * 2. **Meter-first date/time** — uses the clock/calendar read from the meter
 *    screen; falls back to the system date/time when the meter hides them.
 * 3. **Auto session** — Sáng/Trưa/Chiều/Tối from the resolved hour.
 * 4. **Auto save** — writes straight to Room on confirm; the Dashboard flow
 *    then retriggers the full forecast + HbA1c + risk-insight pipeline
 *    (Real-time AI Loop).
 */
@Singleton
class ScanAutoImportPipeline @Inject constructor(
    private val repository: LogRepository
) {

    /**
     * Builds a reviewable draft from a raw scan, or null when the value is
     * OCR noise outside the safe range. Pure except for the injectable
     * clock ([now]) so the fallback path is unit-testable.
     */
    fun buildDraft(result: ScannedGlucoseResult, now: Date = Date()): ScanImportDraft? {
        // Step 1 — Auto Clean: drop noise / OCR errors outside 2.0–30.0.
        if (!GlucosePolicy.isValid(result.value)) return null

        val systemDate = systemDate(now)
        val systemTime = systemTime(now)

        // Step 2 — meter-first date/time with system fallback.
        val resolvedDate = sanitizeDate(result.date) ?: systemDate
        val resolvedTime = sanitizeTime(result.time) ?: systemTime

        // Step 3 — auto session from the resolved hour.
        val session = SessionClassifier.fromTime(resolvedTime)

        // Step 4 — instant risk + provisional HbA1c for the review banner.
        val risk = GlucoseRiskEvaluator.evaluate(result.value)

        return ScanImportDraft(
            value = result.value,
            date = resolvedDate,
            time = resolvedTime,
            session = session,
            dateSource = if (sanitizeDate(result.date) != null) ScanDateSource.METER else ScanDateSource.SYSTEM,
            timeSource = if (sanitizeTime(result.time) != null) ScanTimeSource.METER else ScanTimeSource.SYSTEM,
            risk = risk,
            hba1cProvisional = Hba1cEstimator.fromSingleReading(result.value),
            rawText = result.rawText,
            confidence = result.confidence,
            scannerSource = result.source
        )
    }

    /**
     * Persists a confirmed draft. Guards against double-saving the same
     * scan and fills the empty glucose slot of an existing session row
     * (deterministic: `bgBefore` first, then `bgAfter`, then overwrite
     * `bgAfter` with the newest reading).
     */
    suspend fun confirmAndSave(draft: ScanImportDraft): ScanSaveResult {
        if (!GlucosePolicy.isValid(draft.value)) {
            return ScanSaveResult.Rejected(
                "Giá trị ${draft.value} nằm ngoài ngưỡng an toàn (2.0–30.0 mmol/L)."
            )
        }

        val existingEntries = repository.getLogsByDate(draft.date).first()
        val scannedValue = draft.value.toDouble()

        val duplicate = existingEntries.find {
            it.session == draft.session &&
                (it.bgBefore == scannedValue || it.bgAfter == scannedValue) &&
                it.time == draft.time
        }
        if (duplicate != null) {
            return ScanSaveResult.Duplicate(duplicate, draft)
        }

        val existing = existingEntries.find { it.session == draft.session }
        if (existing != null) {
            val updated = when {
                existing.bgBefore == null ->
                    existing.copy(bgBefore = scannedValue, time = draft.time)
                existing.bgAfter == null ->
                    existing.copy(bgAfter = scannedValue, time = draft.time)
                else ->
                    existing.copy(bgAfter = scannedValue, time = draft.time)
            }
            repository.insertLog(updated)
            return ScanSaveResult.Saved(updated, draft, isUpdate = true)
        }

        val entry = LogEntry(
            date = draft.date,
            session = draft.session,
            time = draft.time,
            bgBefore = scannedValue,
            note = "Quét AI Camera (${draft.scannerSource})"
        )
        repository.insertLog(entry)
        return ScanSaveResult.Saved(entry, draft, isUpdate = false)
    }

    private fun systemDate(now: Date): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)

    private fun systemTime(now: Date): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

    /**
     * Strictly validates a normalized `yyyy-MM-dd` date (also repairs the
     * `yyyy-dd-MM` swap some meters produce). Returns null when the meter
     * value is unusable so the caller falls back to the system date.
     */
    internal fun sanitizeDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val parts = raw.trim().split('-')
            if (parts.size != 3) return null
            val year = parts[0].toInt()
            var month = parts[1].toInt()
            var day = parts[2].toInt()
            if (month > 12 && day <= 12) {
                val tmp = month
                month = day
                day = tmp
            }
            if (year !in 1990..2100 || month !in 1..12 || day !in 1..31) return null
            "%04d-%02d-%02d".format(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    /** Strictly validates a normalized `HH:mm` time for the fallback path. */
    internal fun sanitizeTime(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val parts = raw.trim().split(':')
            if (parts.size != 2) return null
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            if (hour !in 0..23 || minute !in 0..59) return null
            "%02d:%02d".format(hour, minute)
        } catch (_: Exception) {
            null
        }
    }
}
