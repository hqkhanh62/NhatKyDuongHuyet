package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.repository.LogRepository
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern
import com.example.nhatkyduonghuyet.ui.chart.aggregateBySession
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
    val isBetter: Boolean = true 
)

data class ChartPointPro(
    val xIndex: Int,
    val value: Double,
    val dateLabel: String
)

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

    private fun calculateHbA1c(avgGlucoseMmol: Double): Double {
        return if (avgGlucoseMmol > 0) (avgGlucoseMmol + 2.59) / 1.59 else 0.0
    }

    private fun calculateMetrics(entries: List<LogEntry>): Quad<Double, Double, Int, Double> {
        val values = entries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
        val max = values.maxOrNull() ?: 0.0
        val avg = if (values.isNotEmpty()) values.average() else 0.0
        val highRate = if (values.isNotEmpty()) (values.count { it > 10.0 } * 100 / values.size) else 0
        val hba1c = calculateHbA1c(avg)
        return Quad(max, avg, highRate, hba1c)
    }

    private fun getComparison(current: Double, previous: Double): ComparisonData? {
        if (previous == 0.0) return null
        val diff = current - previous
        val percent = (diff / previous) * 100
        return ComparisonData(
            diff = diff,
            percentChange = percent,
            isBetter = diff <= 0 
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getAllEntries(),
        _timeFilter
    ) { allEntries, filter ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
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
                d != null && d.after(previousLimit) && (d.before(currentLimit) || it.date == sdf.format(currentLimit))
            } catch (e: Exception) { false }
        }

        val (max, avg, highRate, hba1c) = calculateMetrics(currentEntries)
        val (pMax, pAvg, pHighRate, pHba1c) = calculateMetrics(previousEntries)

        // Overlay Chart Data Generation - Filter days with >= 2 measurements
        val currentPoints = aggregateBySession(currentEntries)
            .filter { it.avgDaily != null }
            .mapIndexed { index, p -> 
                ChartPointPro(index, p.avgDaily!!, p.dateLabel)
            }

        val prevPoints = aggregateBySession(previousEntries)
            .filter { it.avgDaily != null }
            .mapIndexed { index, p -> 
                ChartPointPro(index, p.avgDaily!!, p.dateLabel)
            }

        DashboardUiState(
            entries = currentEntries,
            max = max,
            maxCompare = getComparison(max, pMax),
            avg = avg,
            avgCompare = getComparison(avg, pAvg),
            highRate = highRate,
            highRateCompare = getComparison(highRate.toDouble(), pHighRate.toDouble()),
            hba1c = hba1c,
            hba1cCompare = getComparison(hba1c, pHba1c),
            currentPeriodPoints = currentPoints,
            previousPeriodPoints = prevPoints,
            insights = detectRisk(currentEntries),
            currentFilter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}

data class Quad<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
