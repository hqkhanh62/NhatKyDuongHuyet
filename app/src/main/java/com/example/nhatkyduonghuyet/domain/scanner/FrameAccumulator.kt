package com.example.nhatkyduonghuyet.domain.scanner

import com.example.nhatkyduonghuyet.ml.StableReadingTracker

/**
 * Bộ gom frame cho vòng quét realtime: mỗi frame đi qua Auto Clean trước rồi
 * mới vào bộ bỏ phiếu [StableReadingTracker].
 *
 * Tách riêng khỏi ViewModel để logic "khi nào thì chịu khó tin AI" test được
 * trên JVM mà không cần camera.
 */
sealed interface FrameOutcome {
    /** Frame không có gì để đọc (chưa có số, hoặc máy đo báo lỗi). */
    data class Nothing(val reason: AutoImportPipeline.RejectReason, val detail: String?) : FrameOutcome

    /** Đã đọc được số nhưng chưa đủ số lần lặp lại để khoá giá trị. */
    data class Progress(val hits: Int, val required: Int, val liveValue: Float) : FrameOutcome

    /** Chỉ số đã ổn định và sạch - sẵn sàng cho Auto Import. */
    data class Locked(val stableValue: Float, val hits: Int, val liveValue: Float) : FrameOutcome
}

class FrameAccumulator(
    private val tracker: StableReadingTracker = StableReadingTracker()
) {
    private var lastLiveValue: Float? = null

    val requiredMatches: Int get() = tracker.requiredMatches

    /** Giá trị gần nhất AI đọc được (để hiển thị live, chưa chắc đã ổn định). */
    fun lastLiveValue(): Float? = lastLiveValue

    @Synchronized
    fun accept(raw: Float?, meterErrorCode: String? = null): FrameOutcome {
        when (val cleaned = AutoImportPipeline.clean(raw, meterErrorCode)) {
            is AutoImportPipeline.CleanResult.Rejected -> {
                // Một frame lỗi xoá phiếu cũ: đang 5.7 5.7 3.1 thì không được
                // tính là "ổn định".
                tracker.clear()
                lastLiveValue = null
                return FrameOutcome.Nothing(cleaned.reason, cleaned.detail)
            }

            is AutoImportPipeline.CleanResult.Accepted -> {
                lastLiveValue = cleaned.value
                val stable = tracker.offer(cleaned.value)
                val hits = tracker.matchCount()
                return if (stable == null) {
                    FrameOutcome.Progress(hits = hits, required = tracker.requiredMatches, liveValue = cleaned.value)
                } else {
                    FrameOutcome.Locked(stableValue = stable, hits = hits, liveValue = cleaned.value)
                }
            }
        }
    }

    @Synchronized
    fun reset() {
        tracker.clear()
        lastLiveValue = null
    }
}
