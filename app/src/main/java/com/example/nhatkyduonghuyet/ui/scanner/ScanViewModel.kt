package com.example.nhatkyduonghuyet.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.ai.PredictionOutcome
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.ai.RealtimePredictor
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.repository.AIRepository
import com.example.nhatkyduonghuyet.domain.GlucosePolicy
import com.example.nhatkyduonghuyet.domain.health.GlucoseMetrics
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.domain.scanner.AutoImportPipeline
import com.example.nhatkyduonghuyet.domain.scanner.FieldSource
import com.example.nhatkyduonghuyet.domain.scanner.FrameAccumulator
import com.example.nhatkyduonghuyet.domain.scanner.FrameOutcome
import com.example.nhatkyduonghuyet.domain.scanner.GlucoseSession
import com.example.nhatkyduonghuyet.domain.scanner.ReadingSlot
import com.example.nhatkyduonghuyet.domain.scanner.ScanInsight
import com.example.nhatkyduonghuyet.domain.scanner.ScanInsightEngine
import com.example.nhatkyduonghuyet.ml.MeterDisplayFields
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.round

/**
 * Bộ điều phối luồng "Pro AI" của camera:
 *
 * ```
 * frame -> Auto Clean -> Auto Import (ngày/giờ/buổi/ô) -> banner kiểm tra
 *       -> (xác nhận hoặc auto-save) -> Room -> LSTM refresh -> HbA1c + cảnh báo
 * ```
 *
 * Mọi quyết định nghiệp vụ nằm trong [AutoImportPipeline] và
 * [ScanInsightEngine] (Kotlin thuần, test được trên JVM); ViewModel chỉ giữ
 * trạng thái UI và gọi repository.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: LogRepository,
    private val realtimePredictor: RealtimePredictor,
    private val aiRepository: AIRepository
) : ViewModel() {

    private val accumulator = FrameAccumulator()

    /** Kết quả OCR đa tầng của frame gần nhất (giờ/ngày/văn bản nhận dạng được). */
    private var lastFields: MeterDisplayFields = MeterDisplayFields()

    /** Ảnh chụp toàn bộ nhật ký tại thời điểm khoá chỉ số, dùng cho HbA1c + dự báo. */
    private var lastEntries: List<LogEntry> = emptyList()

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    val requiredMatches: Int get() = accumulator.requiredMatches

    /** Mỗi frame OCR: cập nhật chip giờ/ngày + gợi ý lỗi máy đo. */
    fun onOcrFields(fields: MeterDisplayFields) {
        lastFields = fields
        _state.update { state ->
            state.copy(
                overlay = state.overlay.copy(
                    meterTime = fields.time?.formatted,
                    meterDate = fields.date?.dayMonth,
                    hint = fields.errorCode?.let { code -> "Máy đo đang báo lỗi $code" }
                        ?: if (fields.glucose == null) "Chưa thấy chỉ số - giữ yên màn hình trong khung" else null
                )
            )
        }
    }

    /** Giá trị mà pixel reader + ML Kit đã thống nhất cho một frame. */
    fun onFrame(result: ScannedGlucoseResult) {
        if (!_state.value.shouldAnalyze) return
        when (val outcome = accumulator.accept(result.value, lastFields.errorCode)) {
            is FrameOutcome.Nothing -> _state.update { state ->
                state.copy(
                    statusMessage = rejectionMessage(outcome.reason, outcome.detail),
                    overlay = state.overlay.copy(locked = false, hits = 0, liveValue = null)
                )
            }

            is FrameOutcome.Progress -> _state.update { state ->
                state.copy(
                    statusMessage = "Đang khoá chỉ số…",
                    overlay = state.overlay.copy(
                        locked = false,
                        hits = outcome.hits,
                        required = outcome.required,
                        liveValue = AutoImportPipeline.formatMmol(outcome.liveValue)
                    )
                )
            }

            is FrameOutcome.Locked -> lockReading(result, outcome.stableValue)
        }
    }

    private fun lockReading(result: ScannedGlucoseResult, stableValue: Float) {
        _state.update { state ->
            state.copy(
                overlay = state.overlay.copy(
                    locked = true,
                    hits = requiredMatches,
                    required = requiredMatches,
                    liveValue = AutoImportPipeline.formatMmol(stableValue)
                ),
                statusMessage = null
            )
        }
        viewModelScope.launch {
            val draft = buildDraft(result, stableValue)
            val forecast = provisionalForecastResult(draft)
            val insight = analyze(draft, forecast)
            if (_state.value.autoSave) {
                persist(draft, insight)
            } else {
                _state.update { state ->
                    state.copy(
                        phase = ScanPhase.Review(draft = draft, insight = insight, forecast = forecast),
                        statusMessage = null
                    )
                }
            }
        }
    }

    private suspend fun buildDraft(
        result: ScannedGlucoseResult,
        stableValue: Float
    ): AutoImportPipeline.ScanDraft {
        val now = Date()
        val systemDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val systemTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val fields = if (result.fields.glucose != null || result.fields.time != null || result.fields.date != null) {
            result.fields
        } else {
            lastFields
        }
        lastEntries = repository.getAllLogs().first()
        return AutoImportPipeline.draft(
            value = stableValue,
            fields = fields,
            systemDate = systemDate,
            systemTime = systemTime,
            existingForDate = lastEntries,
            valueSource = result.source,
            confidence = result.confidence
        )
    }

    private suspend fun analyze(
        draft: AutoImportPipeline.ScanDraft,
        forecast: PredictionResult?
    ): ScanInsight = ScanInsightEngine.analyze(
        value = draft.value,
        session = draft.session,
        slot = draft.slot,
        entries = lastEntries,
        date = draft.date,
        forecastNext = forecast?.next
    )

    /** Dự báo 6 giờ tới tính tạm trên lịch sử + chỉ số vừa quét (chưa lưu). */
    private suspend fun provisionalForecastResult(
        draft: AutoImportPipeline.ScanDraft
    ): PredictionResult? {
        val history = GlucoseMetrics.chronologicalMeasurements(lastEntries) + draft.value
        val outcome = realtimePredictor.predictStateless(history)
        return if (outcome is PredictionOutcome.Success) outcome.value else null
    }

    /** Người dùng bấm "Xác nhận & lưu". */
    fun confirm() {
        val review = _state.value.phase as? ScanPhase.Review ?: return
        if (review.isSaving) return
        _state.update { state ->
            val phase = state.phase as? ScanPhase.Review ?: return@update state
            state.copy(phase = phase.copy(isSaving = true))
        }
        viewModelScope.launch {
            persist(review.draft, review.insight ?: analyze(review.draft, review.forecast))
        }
    }

    private suspend fun persist(
        draft: AutoImportPipeline.ScanDraft,
        insight: ScanInsight?
    ) {
        _state.update { it.copy(isBusy = true, statusMessage = "Đang lưu vào nhật ký…") }
        // Đọc lại nhật ký tại thời điểm lưu: người dùng có thể vừa sửa tay ô khác.
        lastEntries = repository.getAllLogs().first()
        when (val plan = AutoImportPipeline.plan(draft, lastEntries)) {
            is AutoImportPipeline.ImportPlan.Duplicate -> {
                accumulator.reset()
                // Tách lời gọi suspend ra ngoài update {} để khối cập nhật state
                // chỉ còn tính toán thuần tuý.
                val previewForecast = provisionalForecastResult(draft)
                _state.update { state ->
                    state.copy(
                        isBusy = false,
                        phase = ScanPhase.Review(
                            draft = draft,
                            insight = insight,
                            forecast = previewForecast
                        ),
                        statusMessage = "Chỉ số ${draft.valueText} mmol/L lúc ${draft.time} đã có trong nhật ký."
                    )
                }
            }

            is AutoImportPipeline.ImportPlan.Insert ->
                saveEntry(draft, plan.entry, null, insight, lastEntries)

            is AutoImportPipeline.ImportPlan.Update ->
                saveEntry(draft, plan.entry, plan.previous, insight, lastEntries)
        }
    }

    /**
     * Ghi DB rồi chạy ngay vòng AI: buffer LSTM -> dự báo -> HbA1c -> cảnh báo,
     * đồng thời cập nhật hiệu chỉnh model như các luồng nhập liệu khác.
     */
    private suspend fun saveEntry(
        draft: AutoImportPipeline.ScanDraft,
        entry: LogEntry,
        previous: LogEntry?,
        insightBeforeSave: ScanInsight?,
        historyBeforeSave: List<LogEntry>
    ) {
        repository.insertLog(entry)

        val allEntries = repository.getAllLogs().first()
        lastEntries = allEntries
        // Buffer LSTM phải nhìn thấy đúng bản ghi vừa lưu.
        val measurements = GlucoseMetrics.chronologicalMeasurements(allEntries)
        val outcome = realtimePredictor.refresh(measurements)
        val forecast = if (outcome is PredictionOutcome.Success) outcome.value else null
        val nextPrediction = forecast?.nextPrediction
        // Cảnh báo + HbA1c tính trên lịch sử CHƯA gồm bản ghi mới, vì
        // ScanInsightEngine tự cộng chỉ số vừa quét vào ngày đang lưu: dùng
        // dữ liệu sau khi lưu sẽ tính hai lần cùng một giá trị.
        val insight = insightBeforeSave ?: ScanInsightEngine.analyze(
            value = draft.value,
            session = draft.session,
            slot = draft.slot,
            entries = historyBeforeSave,
            date = draft.date,
            forecastNext = nextPrediction?.next
        )

        val retrainRequested = aiRepository.checkRetrainStatus()
        aiRepository.autoCalibrate()

        _state.update { state ->
            state.copy(
                isBusy = false,
                statusMessage = null,
                retrainRequested = retrainRequested,
                phase = ScanPhase.Saved(
                    draft = draft,
                    entry = entry,
                    insight = insight,
                    forecast = nextPrediction,
                    previousEntry = previous
                ),
                overlay = state.overlay.copy(
                    locked = true,
                    hits = requiredMatches,
                    required = requiredMatches
                )
            )
        }
    }

    /** Hoàn tác bản ghi vừa lưu tự động (Undo) - quay lại bước kiểm tra. */
    fun undo() {
        val saved = _state.value.phase as? ScanPhase.Saved ?: return
        viewModelScope.launch {
            val restore = saved.previousEntry
            if (restore == null) repository.deleteLog(saved.entry) else repository.insertLog(restore)
            lastEntries = repository.getAllLogs().first()
            accumulator.reset()
            _state.update { state ->
                state.copy(
                    phase = ScanPhase.Review(draft = saved.draft, insight = saved.insight),
                    statusMessage = "Đã hoàn tác - chưa lưu vào nhật ký."
                )
            }
        }
    }

    /** Quét lại từ đầu (bỏ phiếu cũ, bật lại vòng phân tích camera). */
    fun scanAnother() {
        accumulator.reset()
        _state.update { state ->
            state.copy(
                phase = ScanPhase.Scanning,
                statusMessage = null,
                retrainRequested = false,
                overlay = ScanOverlayState(required = requiredMatches)
            )
        }
    }

    fun setAutoSave(enabled: Boolean) {
        _state.update { it.copy(autoSave = enabled) }
    }

    fun consumeRetrainNotice() {
        _state.update { it.copy(retrainRequested = false) }
    }

    // ---------------------------------------------------------------- sửa tay
    fun adjustValue(delta: Float) = updateDraft { it.copy(value = nudged(it.value, delta)) }

    fun setManualValue(raw: String) {
        val parsed = raw.replace(',', '.').toFloatOrNull() ?: return
        updateDraft { it.copy(value = parsed.coerceIn(GlucosePolicy.MIN_GLUCOSE_MMOL, GlucosePolicy.MAX_GLUCOSE_MMOL)) }
    }

    fun selectSession(session: GlucoseSession) = updateDraft { draft ->
        draft.copy(session = session, slot = reslot(draft, session))
    }

    fun selectSlot(slot: ReadingSlot) = updateDraft { it.copy(slot = slot) }

    fun setTimeText(raw: String) {
        val normalized = AutoImportPipeline.normalizeTime(raw) ?: return
        updateDraft { draft ->
            draft.copy(
                time = normalized,
                timeSource = FieldSource.MANUAL,
                session = GlucoseSession.fromTime(normalized) ?: draft.session
            )
        }
    }

    /** Màn hình máy đo không có giờ -> cho phép đổi tạm sang giờ điện thoại. */
    fun useSystemTime() {
        val systemTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        updateDraft { draft ->
            draft.copy(
                time = systemTime,
                timeSource = FieldSource.SYSTEM,
                session = GlucoseSession.fromTime(systemTime) ?: draft.session
            )
        }
    }

    fun setDateText(raw: String) {
        val normalized = AutoImportPipeline.normalizeDate(raw) ?: return
        updateDraft { draft ->
            draft.copy(date = normalized, dateSource = FieldSource.MANUAL)
        }
    }

    /** Sửa tay thì buổi được tính lại theo giờ mới; ô giá trị chỉ đổi khi còn trống. */
    private fun reslot(
        draft: AutoImportPipeline.ScanDraft,
        session: GlucoseSession
    ): ReadingSlot {
        val hour = draft.time.substringBefore(':').toIntOrNull() ?: return draft.slot
        val minute = draft.time.substringAfter(':', "").toIntOrNull() ?: 0
        val existing = AutoImportPipeline.findSessionEntry(lastEntries, draft.date, session)
        return AutoImportPipeline.decideSlot(
            hour = hour,
            minute = minute,
            session = session,
            hasBefore = existing?.bgBefore != null,
            hasAfter = existing?.bgAfter != null
        )
    }

    private fun nudged(value: Float, delta: Float): Float {
        val next = round((value + delta) * 10f) / 10f
        return next.coerceIn(GlucosePolicy.MIN_GLUCOSE_MMOL, GlucosePolicy.MAX_GLUCOSE_MMOL)
    }

    private fun updateDraft(
        transform: (AutoImportPipeline.ScanDraft) -> AutoImportPipeline.ScanDraft
    ) {
        val review = _state.value.phase as? ScanPhase.Review ?: return
        val updated = transform(review.draft)
        _state.update { state ->
            val phase = state.phase as? ScanPhase.Review ?: return@update state
            state.copy(phase = phase.copy(draft = updated))
        }
        refreshInsight(updated)
    }

    /** Tính lại cảnh báo/HbA1c sau khi người dùng sửa, chỉ áp dụng nếu nháp chưa đổi. */
    private fun refreshInsight(draft: AutoImportPipeline.ScanDraft) {
        viewModelScope.launch {
            val forecast = provisionalForecastResult(draft)
            val insight = analyze(draft, forecast)
            _state.update { state ->
                val phase = state.phase as? ScanPhase.Review ?: return@update state
                if (phase.draft != draft) return@update state
                state.copy(phase = phase.copy(insight = insight, forecast = forecast))
            }
        }
    }

    private fun rejectionMessage(
        reason: AutoImportPipeline.RejectReason,
        detail: String?
    ): String = when (reason) {
        AutoImportPipeline.RejectReason.METER_ERROR ->
            "Máy đo đang báo lỗi ${detail.orEmpty()} - chờ màn hình hết lỗi để đọc chỉ số."
        AutoImportPipeline.RejectReason.OUT_OF_RANGE ->
            "Bỏ qua giá trị nhiễu ${detail.orEmpty()} mmol/L (ngoài ngưỡng an toàn " +
                "${AutoImportPipeline.formatMmol(GlucosePolicy.MIN_GLUCOSE_MMOL)}" +
                "-${AutoImportPipeline.formatMmol(GlucosePolicy.MAX_GLUCOSE_MMOL)})."
        AutoImportPipeline.RejectReason.NOT_FINITE -> "Bỏ qua giá trị OCR không hợp lệ."
        AutoImportPipeline.RejectReason.NO_VALUE -> "Chưa đọc được chỉ số từ màn hình."
    }
}
