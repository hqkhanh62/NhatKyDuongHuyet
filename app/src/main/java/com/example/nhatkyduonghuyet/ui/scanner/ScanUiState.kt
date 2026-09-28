package com.example.nhatkyduonghuyet.ui.scanner

import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.scanner.AutoImportPipeline
import com.example.nhatkyduonghuyet.domain.scanner.ScanInsight

/** Trạng thái màn hình quét camera AI. */
data class ScanUiState(
    val phase: ScanPhase = ScanPhase.Scanning,
    val overlay: ScanOverlayState = ScanOverlayState(),
    val autoSave: Boolean = true,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val retrainRequested: Boolean = false
) {
    /** Camera chỉ được phân tích khi đang ở giai đoạn quét và chưa lưu dở. */
    val shouldAnalyze: Boolean get() = phase is ScanPhase.Scanning && !isBusy

    val draft: AutoImportPipeline.ScanDraft?
        get() = when (phase) {
            is ScanPhase.Review -> phase.draft
            is ScanPhase.Saved -> phase.draft
            ScanPhase.Scanning -> null
        }
}

/** Các giai đoạn của luồng "Quét -> Kiểm tra -> Lưu -> Dự báo -> Cảnh báo". */
sealed interface ScanPhase {
    data object Scanning : ScanPhase

    /** Bản nháp đã có đủ chỉ số + ngày/giờ/buổi, chờ người dùng xác nhận. */
    data class Review(
        val draft: AutoImportPipeline.ScanDraft,
        val insight: ScanInsight? = null,
        val forecast: PredictionResult? = null,
        val isSaving: Boolean = false
    ) : ScanPhase

    /** Đã ghi vào nhật ký; banner hiển thị kết quả vòng AI vừa chạy lại. */
    data class Saved(
        val draft: AutoImportPipeline.ScanDraft,
        val entry: LogEntry,
        val insight: ScanInsight,
        val forecast: PredictionResult? = null,
        val previousEntry: LogEntry? = null
    ) : ScanPhase
}
