package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.repository.LogRepository
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class DashboardTimeFilter(val days: Int, val label: String) {
    LAST_15_DAYS(15, "15 ngày qua"),
    LAST_30_DAYS(30, "30 ngày qua"),
    LAST_60_DAYS(60, "60 ngày qua"),
    ALL(Int.MAX_VALUE, "Toàn bộ thời gian")
}

data class ComparisonData(
    val diff: Double = 0.0,
    val percentChange: Double = 0.0,
    val isBetter: Boolean = true // Decreasing glucose is usually better
)

data class DashboardUiState(
    val entries: List<LogEntry> = emptyList(),
    val max: Double = 0.0,
    val maxCompare: ComparisonData? = null,
    val avg: Double = 0.0,
    val avgCompare: ComparisonData? = null,
    val highRate: Int = 0,
    val highRateCompare: ComparisonData? = null,
    val insights: List<String> = emptyList(),
    val currentFilter: DashboardTimeFilter = DashboardTimeFilter.LAST_15_DAYS
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repo: LogRepository,
    private val detectRisk: DetectRiskPattern
) : ViewModel() {

    private val _timeFilter = MutableStateFlow(DashboardTimeFilter.LAST_15_DAYS)
    val timeFilter = _timeFilter.asStateFlow()

    fun setTimeFilter(filter: DashboardTimeFilter) {
        _timeFilter.value = filter
    }

    private fun calculateMetrics(entries: List<LogEntry>): Triple<Double, Double, Int> {
        val values = entries.mapNotNull { it.bgAfter ?: it.bgBefore }
        val max = values.maxOrNull() ?: 0.0
        val avg = if (values.isNotEmpty()) values.average() else 0.0
        val highRate = if (values.isNotEmpty()) (values.count { it > 10.0 } * 100 / values.size) else 0
        return Triple(max, avg, highRate)
    }

    private fun getComparison(current: Double, previous: Double): ComparisonData? {
        if (previous == 0.0) return null
        val diff = current - previous
        val percent = (diff / previous) * 100
        return ComparisonData(
            diff = diff,
            percentChange = percent,
            isBetter = diff <= 0 // Lower is usually better for glucose
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getAllEntries(),
        _timeFilter
    ) { allEntries, filter ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()
        
        val currentLimit = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -filter.days) }.time
        val previousLimit = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -filter.days * 2) }.time

        val currentEntries = allEntries.filter {
            try {
                val d = sdf.parse(it.date)
                d != null && (d.after(currentLimit) || it.date == sdf.format(Date()))
            } catch (e: Exception) { false }
        }

        val previousEntries = if (filter == DashboardTimeFilter.ALL) emptyList() else allEntries.filter {
            try {
                val d = sdf.parse(it.date)
                d != null && d.after(previousLimit) && d.before(currentLimit)
            } catch (e: Exception) { false }
        }

        val (max, avg, highRate) = calculateMetrics(currentEntries)
        val (pMax, pAvg, pHighRate) = calculateMetrics(previousEntries)

        DashboardUiState(
            entries = currentEntries,
            max = max,
            maxCompare = getComparison(max, pMax),
            avg = avg,
            avgCompare = getComparison(avg, pAvg),
            highRate = highRate,
            highRateCompare = getComparison(highRate.toDouble(), pHighRate.toDouble()),
            insights = detectRisk(currentEntries),
            currentFilter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
