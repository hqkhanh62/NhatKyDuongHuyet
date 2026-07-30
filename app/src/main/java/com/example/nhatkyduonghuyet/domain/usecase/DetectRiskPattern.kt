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

        val values = entries.mapNotNull { it.bgAfter }
        val avg = if (values.isNotEmpty()) values.average() else 0.0
        
        if (avg > 9.0) {
            insights.add("📊 Trung bình cao → nguy cơ HbA1c cao")
        }

        // Need at least 2 points to check trend
        if (entries.size >= 3) {
            val lastValues = entries.takeLast(3).mapNotNull { it.bgAfter }
            if (lastValues.size >= 2) {
                val trendUp = lastValues.zipWithNext().all { it.second > it.first }
                if (trendUp) {
                    insights.add("📈 Xu hướng tăng liên tục")
                }
            }
        }

        return insights
    }
}
