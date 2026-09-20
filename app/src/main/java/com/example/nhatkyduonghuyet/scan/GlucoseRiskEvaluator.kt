package com.example.nhatkyduonghuyet.scan

import com.example.nhatkyduonghuyet.domain.GlucosePolicy
import com.example.nhatkyduonghuyet.domain.GlucoseRiskLevel

/**
 * Instant risk evaluation for a scanned value (Real-time AI Loop:
 * Scan → Save → Forecast → Warning).
 */
data class GlucoseRisk(
    val level: GlucoseRiskLevel,
    val message: String,
    /** True when the value needs an immediate danger alert (flash + SOS vibration). */
    val isDanger: Boolean
)

object GlucoseRiskEvaluator {

    fun evaluate(value: Float): GlucoseRisk = when {
        value < GlucosePolicy.LOW_THRESHOLD -> GlucoseRisk(
            level = GlucoseRiskLevel.LOW,
            message = "⚠️ Hạ đường huyết — cần bổ sung đường ngay!",
            isDanger = true
        )
        value > GlucosePolicy.VERY_HIGH_THRESHOLD -> GlucoseRisk(
            level = GlucoseRiskLevel.VERY_HIGH,
            message = "🚨 Đường huyết rất cao — nguy cơ biến chứng!",
            isDanger = true
        )
        value > GlucosePolicy.HIGH_THRESHOLD -> GlucoseRisk(
            level = GlucoseRiskLevel.HIGH,
            message = "⚠️ Đường huyết cao — cần theo dõi chặt chẽ.",
            isDanger = false
        )
        else -> GlucoseRisk(
            level = GlucoseRiskLevel.NORMAL,
            message = "✅ Chỉ số trong ngưỡng an toàn.",
            isDanger = false
        )
    }
}
