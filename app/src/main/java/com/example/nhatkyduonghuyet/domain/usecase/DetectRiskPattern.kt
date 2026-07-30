package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import javax.inject.Inject

class DetectRiskPattern @Inject constructor() {

    operator fun invoke(entries: List<LogEntry>): List<String> {
        val insights = mutableListOf<String>()

        val highs = entries.filter { (it.bgAfter ?: 0.0) > 13.0 }
        if (highs.size >= 2) {
            insights.add("🚨 Nhiều lần vượt mức nguy hiểm (>13)")
        }

        val avg = entries.mapNotNull { it.bgAfter }.average()
        if (avg > 9.0) {
            insights.add("📊 Trung bình cao → nguy cơ HbA1c cao")
        }

        val trendUp = if (entries.size >= 3) {
            entries.takeLast(3)
                .mapNotNull { it.bgAfter }
                .zipWithNext()
                .all { it.second > it.first }
        } else false

        if (trendUp) {
            insights.add("📈 Xu hướng tăng liên tục")
        }

        return insights
    }
}
