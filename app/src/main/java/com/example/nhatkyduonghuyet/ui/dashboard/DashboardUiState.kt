package com.example.nhatkyduonghuyet.ui.dashboard

import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry

enum class DashboardTimeFilter(val days: Int, val label: String) {
    LAST_15_DAYS(15, "15 ngày qua"),
    LAST_30_DAYS(30, "30 ngày qua"),
    LAST_60_DAYS(60, "60 ngày qua"),
    ALL(Int.MAX_VALUE, "Toàn bộ thời gian")
}

data class ComparisonData(
    val diff: Double = 0.0,
    val percentChange: Double = 0.0,
    val isBetter: Boolean = true
)

data class ChartPointPro(
    val xIndex: Int,
    val value: Double,
    val dateLabel: String
)

sealed interface GeminiInsightUiState {
    data object Idle : GeminiInsightUiState
    data object Loading : GeminiInsightUiState
    data class Content(val text: String) : GeminiInsightUiState
    data class Unavailable(val message: String) : GeminiInsightUiState
}

data class DashboardUiState(
    val entries: List<LogEntry> = emptyList(),
    val max: Double = 0.0,
    val maxCompare: ComparisonData? = null,
    val avg: Double = 0.0,
    val avgCompare: ComparisonData? = null,
    val highRate: Int = 0,
    val highRateCompare: ComparisonData? = null,
    val hba1c: Double = 0.0,
    val hba1cCompare: ComparisonData? = null,
    val currentPeriodPoints: List<ChartPointPro> = emptyList(),
    val previousPeriodPoints: List<ChartPointPro> = emptyList(),
    val insights: List<String> = emptyList(),
    val currentFilter: DashboardTimeFilter = DashboardTimeFilter.LAST_15_DAYS,
    val realtimePrediction: PredictionResult? = null,
    val multiStepForecast: MultiStepResult? = null,
    val forecastStatus: String? = null,
    val geminiInsight: GeminiInsightUiState = GeminiInsightUiState.Idle
)