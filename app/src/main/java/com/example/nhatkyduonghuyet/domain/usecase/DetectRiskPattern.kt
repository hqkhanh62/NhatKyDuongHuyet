package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import javax.inject.Inject

import com.example.nhatkyduonghuyet.domain.GlucosePolicy

class DetectRiskPattern @Inject constructor() {

    fun detect(entries: List<LogEntry>, smartDailyAverages: List<Float>): List<String> {
        val insights = mutableListOf<String>()

        // 1. Historical Highs
        val highs = entries.filter { (it.bgAfter ?: 0.0) > GlucosePolicy.VERY_HIGH_THRESHOLD || (it.bgBefore ?: 0.0) > GlucosePolicy.VERY_HIGH_THRESHOLD }
        if (highs.size >= 2) {
            insights.add("🚨 Nhiều lần vượt mức nguy hiểm (>${GlucosePolicy.VERY_HIGH_THRESHOLD})")
        }

        // 2. Global Average Insight
        val allValues = entries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
        val avg = if (allValues.isNotEmpty()) allValues.average() else 0.0
        if (avg > GlucosePolicy.HIGH_THRESHOLD) {
            insights.add("📊 Trung bình cao (>${GlucosePolicy.HIGH_THRESHOLD}) → nguy cơ HbA1c cao")
        }

        // 3. AI Trend Forecasting
        if (smartDailyAverages.size >= 3) {
            val recent = smartDailyAverages.takeLast(3)
            val trendUp = recent.zipWithNext().all { it.second > it.first }
            
            // Check if the growth rate is dangerous
            val growthRate = (recent.last() - recent.first()) / recent.first()
            if (trendUp && growthRate > 0.15) {
                insights.add("⚠️ AI CẢNH BÁO: Chỉ số đang tăng nhanh (+${(growthRate * 100).toInt()}%)")
            } else if (trendUp) {
                insights.add("📈 Xu hướng đang tăng dần")
            }

            // Predictive threshold warning
            if (recent.last() > GlucosePolicy.VERY_HIGH_THRESHOLD) {
                insights.add("🚩 DỰ BÁO: Nguy cơ sắp chạm ngưỡng Critical (>${GlucosePolicy.VERY_HIGH_THRESHOLD})")
            }
        }

        return insights
    }
}
