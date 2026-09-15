package com.example.nhatkyduonghuyet.domain.scanner

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.GlucosePolicy
import com.example.nhatkyduonghuyet.ml.MeterDisplayFields
import java.util.Calendar
import java.util.Locale
import kotlin.math.round

/**
 * Luồng tự động hoá sau khi AI đọc xong màn hình máy đo:
 *
 * ```
 * OCR đa tầng -> Auto Clean -> Giờ/Ngày (máy đo, nếu không có thì hệ thống)
 *             -> tự phân loại Buổi + ô Trước/Sau ăn -> Lên kế hoạch lưu
 * ```
 *
 * Toàn bộ là hàm thuần, không đụng Android/Room nên kiểm chứng được bằng
 * JVM unit test (`AutoImportPipelineTest`).
 */
object AutoImportPipeline {

    /** Lý do từ chối một frame, hiển thị trực tiếp cho người dùng. */
    enum class RejectReason { NO_VALUE, NOT_FINITE, OUT_OF_RANGE, METER_ERROR }

    sealed interface CleanResult {
        data class Accepted(val value: Float) : CleanResult
        data class Rejected(val reason: RejectReason, val detail: String? = null) : CleanResult
    }

    /** Thông tin ngày/giờ đã quyết định dùng, kèm nguồn để in lên banner. */
    data class ResolvedTime(val time: String, val source: FieldSource)
    data class ResolvedDate(
        val date: String,
        val source: FieldSource,
        val ambiguous: Boolean = false
    )

    /** Bản ghi nháp đang chờ người dùng xác nhận (hoặc được lưu ngay khi bật Auto-Save). */
    data class ScanDraft(
        val value: Float,
        val date: String,
        val time: String,
        val session: GlucoseSession,
        val slot: ReadingSlot,
        val dateSource: FieldSource,
        val timeSource: FieldSource,
        val valueSource: String,
        val confidence: Float,
        val ocrText: String,
        val fields: MeterDisplayFields = MeterDisplayFields()
    ) {
        val valueText: String get() = formatMmol(value)
        val dateText: String get() = date.split("-").let { parts ->
            if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else date
        }
        val isTrusted: Boolean get() = confidence >= TRUSTED_CONFIDENCE

        private companion object {
            const val TRUSTED_CONFIDENCE = 0.7f
        }
    }

    /** Kế hoạch ghi database. `Duplicate` nghĩa là không cần lưu gì thêm. */
    sealed interface ImportPlan {
        data class Insert(val entry: LogEntry) : ImportPlan
        data class Update(val entry: LogEntry, val previous: LogEntry) : ImportPlan
        data class Duplicate(val existing: LogEntry) : ImportPlan
    }

    /**
     * Auto Clean: loại chỉ số nhiễu/ngoài ngưỡng an toàn 2.0 - 30.0 mmol/L.
     * Giá trị được làm tròn về 0.1 (độ phân giải thật của máy đo).
     */
    fun clean(raw: Float?, meterErrorCode: String? = null): CleanResult {
        if (raw == null) {
            return if (meterErrorCode.isNullOrBlank()) {
                CleanResult.Rejected(RejectReason.NO_VALUE)
            } else {
                CleanResult.Rejected(RejectReason.METER_ERROR, meterErrorCode)
            }
        }
        if (!raw.isFinite()) return CleanResult.Rejected(RejectReason.NOT_FINITE)
        val quantized = round(raw * 10f) / 10f
        if (quantized < GlucosePolicy.MIN_GLUCOSE_MMOL || quantized > GlucosePolicy.MAX_GLUCOSE_MMOL) {
            return CleanResult.Rejected(RejectReason.OUT_OF_RANGE, formatMmol(quantized))
        }
        return CleanResult.Accepted(quantized)
    }

    /** Ưu tiên giờ trên máy đo; màn hình không hiển thị giờ thì dùng giờ hệ thống. */
    fun resolveTime(fields: MeterDisplayFields, systemTime: String): ResolvedTime {
        val meterTime = fields.time?.formatted
        return if (meterTime != null) {
            ResolvedTime(meterTime, FieldSource.METER)
        } else {
            ResolvedTime(normalizeTime(systemTime) ?: systemTime, FieldSource.SYSTEM)
        }
    }

    /** Ưu tiên ngày trên máy đo (đã được MeterTextParser kiểm tra tính hợp lệ). */
    fun resolveDate(fields: MeterDisplayFields, systemDate: String): ResolvedDate {
        val meterDate = fields.date?.iso
        return if (meterDate != null) {
            ResolvedDate(meterDate, FieldSource.METER, fields.date?.ambiguous == true)
        } else {
            ResolvedDate(systemDate, FieldSource.SYSTEM)
        }
    }

    /** Ghép mọi suy luận tự động thành một bản nháp hoàn chỉnh. */
    fun draft(
        value: Float,
        fields: MeterDisplayFields,
        systemDate: String,
        systemTime: String,
        existingForDate: List<LogEntry>,
        valueSource: String = "ML_KIT",
        confidence: Float = fields.glucose?.confidence ?: 0f
    ): ScanDraft {
        val resolvedTime = resolveTime(fields, systemTime)
        val resolvedDate = resolveDate(fields, systemDate)
        val hour = resolvedTime.time.substringBefore(':').toIntOrNull()?.coerceIn(0, 23)
            ?: DEFAULT_HOUR
        val minute = resolvedTime.time.substringAfter(':', "").toIntOrNull()?.coerceIn(0, 59) ?: 0
        val session = GlucoseSession.fromHour(hour)
        val entryForSession = findSessionEntry(existingForDate, resolvedDate.date, session)
        val slot = decideSlot(
            hour = hour,
            minute = minute,
            session = session,
            hasBefore = entryForSession?.bgBefore != null,
            hasAfter = entryForSession?.bgAfter != null
        )
        return ScanDraft(
            value = value,
            date = resolvedDate.date,
            time = resolvedTime.time,
            session = session,
            slot = slot,
            dateSource = resolvedDate.source,
            timeSource = resolvedTime.source,
            valueSource = valueSource,
            confidence = confidence,
            ocrText = fields.readableText,
            fields = fields
        )
    }

    /**
     * Tự chọn ô "trước ăn"/"sau ăn 2 giờ" theo giờ đo và mốc bữa ăn của buổi.
     * Nguyên tắc: ưu tiên ô đang trống, chỉ ghi đè khi cửa sổ bữa ăn ép buộc.
     */
    fun decideSlot(
        hour: Int,
        minute: Int,
        session: GlucoseSession,
        hasBefore: Boolean,
        hasAfter: Boolean
    ): ReadingSlot {
        val preferred = preferredSlot(hour, minute, session)
        if (preferred == ReadingSlot.BEFORE_MEAL && !hasBefore) return ReadingSlot.BEFORE_MEAL
        if (preferred == ReadingSlot.AFTER_MEAL && !hasAfter) return ReadingSlot.AFTER_MEAL
        if (!hasBefore) return ReadingSlot.BEFORE_MEAL
        if (!hasAfter) return ReadingSlot.AFTER_MEAL
        return ReadingSlot.AFTER_MEAL
    }

    private fun preferredSlot(hour: Int, minute: Int, session: GlucoseSession): ReadingSlot? {
        val minutes = hour * 60 + minute
        val anchor = session.mealAnchorMinutes
        return when {
            minutes in (anchor - 75)..(anchor + 30) -> ReadingSlot.BEFORE_MEAL
            minutes in (anchor + 60)..(anchor + 180) -> ReadingSlot.AFTER_MEAL
            else -> null
        }
    }

    /** Dịch bản nháp (người dùng có thể đã sửa buổi/ô/giờ) thành kế hoạch ghi DB. */
    fun plan(draft: ScanDraft, existingForDate: List<LogEntry>): ImportPlan {
        val stored = toStoredMmol(draft.value)
        val existing = findSessionEntry(existingForDate, draft.date, draft.session)
        val note = buildNote(draft)

        if (existing == null) {
            val entry = LogEntry(
                date = draft.date,
                session = draft.session.label,
                time = draft.time,
                bgBefore = if (draft.slot.isBefore) stored else null,
                bgAfter = if (draft.slot.isBefore) null else stored,
                note = note
            )
            return ImportPlan.Insert(entry)
        }

        val alreadyRecorded = draft.time.isNotBlank() && existing.time == draft.time &&
            (existing.bgBefore == stored || existing.bgAfter == stored)
        if (alreadyRecorded) return ImportPlan.Duplicate(existing)

        val updated = if (draft.slot.isBefore) {
            existing.copy(bgBefore = stored, time = existing.time ?: draft.time, note = mergeNote(existing.note, note))
        } else {
            existing.copy(bgAfter = stored, time = existing.time ?: draft.time, note = mergeNote(existing.note, note))
        }
        return ImportPlan.Update(entry = updated, previous = existing)
    }

    fun findSessionEntry(
        entries: List<LogEntry>,
        date: String,
        session: GlucoseSession
    ): LogEntry? = entries.firstOrNull { it.date == date && it.session == session.label }

    /** 5.7f (Float) -> 5.7 (Double) để không lưu 5.700000286102295 vào DB. */
    fun toStoredMmol(value: Float): Double = round(value * 10.0) / 10.0

    fun formatMmol(value: Float): String = String.format(Locale.US, "%.1f", value)

    /** "8:5", "08:05", " 8:05 " -> "08:05"; không hợp lệ thì trả null. */
    fun normalizeTime(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        val match = TIME_PATTERN.find(trimmed) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour > 23 || minute > 59) return null
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    /** "15/09/2026", "15-09-2026", "2026-09-15", "15/09" -> "2026-09-15". */
    fun normalizeDate(raw: String?): String? {
        val parts = raw?.trim().orEmpty()
            .split('/', '-', '.', ' ')
            .filter { it.isNotBlank() }
        return when {
            parts.size == 3 && parts[0].length == 4 -> buildIso(parts[0], parts[1], parts[2])
            parts.size == 3 -> buildIso(parts[2], parts[1], parts[0])
            parts.size == 2 -> buildIso(
                Calendar.getInstance().get(Calendar.YEAR).toString(),
                parts[1],
                parts[0]
            )
            else -> null
        }
    }

    private fun buildIso(yearText: String, monthText: String, dayText: String): String? {
        val year = yearText.toIntOrNull() ?: return null
        val month = monthText.toIntOrNull() ?: return null
        val day = dayText.toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31 || year < 2000 || year > 2100) return null
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    private fun buildNote(draft: ScanDraft): String = buildString {
        append("AI Camera OCR (${draft.valueSource})")
        append(" • ").append(draft.session.label).append(" • ").append(draft.slot.label)
        append(" • giờ ").append(if (draft.timeSource == FieldSource.METER) "máy đo" else "hệ thống")
        if (draft.dateSource == FieldSource.METER) append(" • ngày máy đo")
    }

    private fun mergeNote(existing: String?, autoNote: String): String = when {
        existing.isNullOrBlank() -> autoNote
        existing.contains(autoNote) -> existing
        else -> "$existing • $autoNote"
    }

    private val TIME_PATTERN = Regex("(?<!\\d)(\\d{1,2}):(\\d{1,2})(?!\\d)")

    /** Giờ dự phòng khi không đọc được giờ ở bất kỳ nguồn nào (giữa trưa). */
    private const val DEFAULT_HOUR = 12
}
