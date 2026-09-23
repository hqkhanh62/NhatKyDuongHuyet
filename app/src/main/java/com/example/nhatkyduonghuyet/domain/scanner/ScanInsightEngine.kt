package com.example.nhatkyduonghuyet.domain.scanner

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.GlucosePolicy
import com.example.nhatkyduonghuyet.domain.GlucoseRiskLevel
import com.example.nhatkyduonghuyet.domain.health.GlucoseMetrics
import com.example.nhatkyduonghuyet.domain.health.Hba1cEstimate
import java.util.Locale
import kotlin.math.abs

/** Mức độ của một cảnh báo, quyết định màu trên banner. */
enum class AlertLevel { INFO, WARNING, CRITICAL }

data class RiskAlert(val level: AlertLevel, val message: String)

/**
 * Kết quả của vòng "Quét -> Lưu -> Dự báo -> Cảnh báo": chỉ số vừa quét,
 * HbA1c ước tính đã tính lại và danh sách cảnh báo rủi ro.
 */
data class ScanInsight(
    val risk: GlucoseRiskLevel,
    val headline: String,
    val alerts: List<RiskAlert>,
    val hba1c: Hba1cEstimate,
    val previousHba1c: Hba1cEstimate,
    val timeInRangePercent: Int?,
    val forecastNext: Float?,
    val sampleCount: Int
) {
    val isCritical: Boolean get() = alerts.any { it.level == AlertLevel.CRITICAL }
    val hasWarning: Boolean get() = alerts.any { it.level == AlertLevel.WARNING }

    /** %HbA1c thay đổi bao nhiêu so với trước khi lưu bản quét này. */
    val hba1cDelta: Double
        get() = if (hba1c.isAvailable && previousHba1c.isAvailable) hba1c.percent - previousHba1c.percent else 0.0

    val hba1cDeltaText: String?
        get() = if (hba1cDelta == 0.0) null else String.format(Locale.US, "%+.2f", hba1cDelta)

    companion object {
        fun empty() = ScanInsight(
            risk = GlucoseRiskLevel.NORMAL,
            headline = "Chưa có dữ liệu",
            alerts = emptyList(),
            hba1c = Hba1cEstimate(0.0, 0f, 0),
            previousHba1c = Hba1cEstimate(0.0, 0f, 0),
            timeInRangePercent = null,
            forecastNext = null,
            sampleCount = 0
        )
    }
}

/**
 * Bộ sinh cảnh báo rủi ro tức thì cho một chỉ số vừa quét.
 *
 * Đây là lớp "giải thích" dữ liệu: không chẩn đoán, chỉ so sánh với ngưỡng
 * mục tiêu điều trị đã khai báo trong [GlucosePolicy] + xu hướng 7 chỉ số gần
 * nhất, và tính lại HbA1c ước tính ngay trên dữ liệu chưa lưu (nên banner
 * hiển thị được số liệu trước khi người dùng bấm xác nhận).
 */
object ScanInsightEngine {

    /** Hạ đường huyết nặng: cần xử trí ngay. */
    private const val SEVERE_HYPO_MMOL = 3.0f

    /** Ngưỡng sau ăn 2 giờ theo mục tiêu điều trị (mmol/L). */
    private const val POST_MEAL_TARGET_MMOL = 7.8f

    /** Trên ngưỡng này cần nghĩ tới nhiễm toan ceton / tăng áp lực thẩm thấu. */
    private const val VERY_HIGH_MMOL = 16.7f

    /** Đường huyết đói 5.6 - 6.9: ranh giới rối loạn dung nạp glucose. */
    private const val FASTING_IMPAIRED_MIN_MMOL = 5.6f
    private const val FASTING_IMPAIRED_MAX_MMOL = 6.9f

    /** Chênh lệch so với trung bình 7 chỉ số gần nhất bị coi là bất thường. */
    private const val TREND_SHIFT_MMOL = 2.0f
    private const val TREND_WINDOW = 7

    fun analyze(
        value: Float,
        session: GlucoseSession,
        slot: ReadingSlot,
        entries: List<LogEntry>,
        date: String,
        forecastNext: Float? = null
    ): ScanInsight {
        val risk = riskOf(value, slot)
        val updatedAverages = GlucoseMetrics.dailyAveragesWithReading(entries, date, value)
        val hba1c = estimate(updatedAverages)
        val previousHba1c = estimate(GlucoseMetrics.dailyMeasuredAverages(entries))
        val history = GlucoseMetrics.chronologicalMeasurements(entries)
        val recent = history.takeLast(TREND_WINDOW)
        val alerts = buildAlerts(value, session, slot, risk, recent, hba1c, forecastNext)

        return ScanInsight(
            risk = risk,
            headline = headlineOf(risk),
            alerts = alerts,
            hba1c = hba1c,
            previousHba1c = previousHba1c,
            timeInRangePercent = GlucoseMetrics.timeInRangePercent(history + value),
            forecastNext = forecastNext,
            sampleCount = history.size + 1
        )
    }

    private fun estimate(dailyAverages: Map<String, Float>): Hba1cEstimate {
        val values = dailyAverages.values.toList()
        val weighted = GlucoseMetrics.weightedAverage(values)
        return Hba1cEstimate(
            percent = GlucoseMetrics.estimateHba1c(weighted),
            weightedAverageMmol = weighted,
            dayCount = values.size
        )
    }

    private fun riskOf(value: Float, slot: ReadingSlot): GlucoseRiskLevel {
        val upperTarget = if (slot == ReadingSlot.AFTER_MEAL) POST_MEAL_TARGET_MMOL else GlucosePolicy.HIGH_THRESHOLD
        return when {
            value < GlucosePolicy.LOW_THRESHOLD -> GlucoseRiskLevel.LOW
            value > GlucosePolicy.VERY_HIGH_THRESHOLD -> GlucoseRiskLevel.VERY_HIGH
            value > upperTarget -> GlucoseRiskLevel.HIGH
            else -> GlucoseRiskLevel.NORMAL
        }
    }

    private fun headlineOf(risk: GlucoseRiskLevel): String = when (risk) {
        GlucoseRiskLevel.LOW -> "Hạ đường huyết"
        GlucoseRiskLevel.NORMAL -> "Trong mục tiêu"
        GlucoseRiskLevel.HIGH -> "Đường huyết cao"
        GlucoseRiskLevel.VERY_HIGH -> "Nguy hiểm - cần xử trí"
    }

    private fun buildAlerts(
        value: Float,
        session: GlucoseSession,
        slot: ReadingSlot,
        risk: GlucoseRiskLevel,
        recent: List<Float>,
        hba1c: Hba1cEstimate,
        forecastNext: Float?
    ): List<RiskAlert> {
        val alerts = mutableListOf<RiskAlert>()

        when {
            value <= SEVERE_HYPO_MMOL -> alerts += RiskAlert(
                AlertLevel.CRITICAL,
                "Hạ đường huyết nặng (${format(value)} mmol/L). Cần 15-20g đường nhanh, đo lại sau 15 phút."
            )
            value < GlucosePolicy.LOW_THRESHOLD -> alerts += RiskAlert(
                AlertLevel.CRITICAL,
                "Hạ đường huyết (<${format(GlucosePolicy.LOW_THRESHOLD)} mmol/L). Ăn nhẹ và đo lại sau 15 phút."
            )
        }
        if (value > VERY_HIGH_MMOL) {
            alerts += RiskAlert(
                AlertLevel.CRITICAL,
                "Rất cao (${format(value)} mmol/L). Uống đủ nước, kiểm tra ceton và liên hệ y tế."
            )
        } else if (value > GlucosePolicy.VERY_HIGH_THRESHOLD) {
            alerts += RiskAlert(
                AlertLevel.WARNING,
                "Vượt ngưỡng nguy hiểm (>${format(GlucosePolicy.VERY_HIGH_THRESHOLD)} mmol/L)."
            )
        } else if (risk == GlucoseRiskLevel.HIGH) {
            val target = if (slot == ReadingSlot.AFTER_MEAL) POST_MEAL_TARGET_MMOL else GlucosePolicy.HIGH_THRESHOLD
            val label = if (slot == ReadingSlot.AFTER_MEAL) "sau ăn 2 giờ" else "trong ngày"
            alerts += RiskAlert(AlertLevel.WARNING, "Cao hơn mục tiêu $label (>${format(target)} mmol/L).")
        }

        if (slot == ReadingSlot.AFTER_MEAL && value in POST_MEAL_TARGET_MMOL..11.0f) {
            alerts += RiskAlert(
                AlertLevel.INFO,
                "Sau ăn 2 giờ ${format(value)} mmol/L: hơi cao, nên giảm tinh bột trong bữa."
            )
        }
        if (session == GlucoseSession.MORNING && slot == ReadingSlot.BEFORE_MEAL &&
            value in FASTING_IMPAIRED_MIN_MMOL..FASTING_IMPAIRED_MAX_MMOL
        ) {
            alerts += RiskAlert(
                AlertLevel.INFO,
                "Đường huyết đói ${format(value)} mmol/L nằm ở ranh giới rối loạn dung nạp glucose."
            )
        }

        if (recent.size >= 3) {
            val average = recent.average().toFloat()
            val shift = value - average
            if (abs(shift) >= TREND_SHIFT_MMOL) {
                val direction = if (shift > 0) "tăng" else "giảm"
                alerts += RiskAlert(
                    AlertLevel.WARNING,
                    "Bất thường: ${format(abs(shift))} mmol/L $direction so với trung bình ${recent.size} chỉ số gần nhất."
                )
            }
        }

        if (hba1c.isAvailable) {
            when {
                hba1c.percent >= 9.0 -> alerts += RiskAlert(
                    AlertLevel.CRITICAL,
                    "HbA1c ước tính ${format(hba1c.percent)}% - cần đánh giá lại phác đồ điều trị."
                )
                hba1c.isAboveTarget -> alerts += RiskAlert(
                    AlertLevel.WARNING,
                    "HbA1c ước tính ${format(hba1c.percent)}% vượt mục tiêu ${GlucoseMetrics.HBA1C_TARGET_PERCENT}%."
                )
                else -> alerts += RiskAlert(
                    AlertLevel.INFO,
                    "HbA1c ước tính ${format(hba1c.percent)}% - trong mục tiêu kiểm soát."
                )
            }
        }

        if (forecastNext != null && forecastNext.isFinite()) {
            when {
                forecastNext > GlucosePolicy.HIGH_THRESHOLD -> alerts += RiskAlert(
                    AlertLevel.WARNING,
                    "AI dự báo 6 giờ tới ${format(forecastNext)} mmol/L - đo lại trước khi ăn."
                )
                forecastNext < GlucosePolicy.LOW_THRESHOLD -> alerts += RiskAlert(
                    AlertLevel.WARNING,
                    "AI dự báo 6 giờ tới ${format(forecastNext)} mmol/L - chuẩn bị đồ ăn nhẹ."
                )
            }
        }

        if (alerts.isEmpty()) {
            alerts += RiskAlert(AlertLevel.INFO, "Chỉ số trong ngưỡng mục tiêu.")
        }
        return alerts
    }

    private fun format(value: Float): String = String.format(Locale.US, "%.1f", value)

    private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)
}
