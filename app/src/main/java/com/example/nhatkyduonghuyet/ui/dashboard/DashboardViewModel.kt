package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.repository.LogRepository
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
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
    private val repo: LogRepository,
    private val predictor: GlucosePredictor,
    private val detectRisk: DetectRiskPattern
) : ViewModel() {

    private val _timeFilter = MutableStateFlow(DashboardTimeFilter.LAST_15_DAYS)
    val timeFilter = _timeFilter.asStateFlow()

    fun setTimeFilter(filter: DashboardTimeFilter) {
        _timeFilter.value = filter
    }

    private fun estimateDailyAvg(fasting: Float): Float {
        val noon = predictor.predict(fasting, 0)
        val evening = predictor.predict(fasting, 1)
        return (fasting + noon + evening) / 3f
    }

    private fun getSmartDailyAverages(entries: List<LogEntry>): Map<String, Float> {
        val groupedByDate = entries.groupBy { it.date }.toSortedMap()
        return groupedByDate.mapValues { (_, dayEntries) ->
            val fastingEntry = dayEntries.find { it.session == "Sáng" && it.bgBefore != null }
            if (fastingEntry != null) {
                estimateDailyAvg(fastingEntry.bgBefore!!.toFloat())
            } else {
                val dayValues = dayEntries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
                if (dayValues.isNotEmpty()) dayValues.average().toFloat() else 0f
            }
        }.filter { it.value > 0f }
    }

    private fun weightedAverage(glucoseList: List<Float>): Float {
        if (glucoseList.isEmpty()) return 0f
        val weights = glucoseList.mapIndexed { i, _ -> (i + 1).toFloat() }
        val totalWeight = weights.sum()
        val weightedSum = glucoseList.zip(weights).sumOf { (it.first * it.second).toDouble() }
        return (weightedSum / totalWeight).toFloat()
    }

    private fun calculateHbA1c(weightedAvgGlucose: Float): Double {
        return if (weightedAvgGlucose > 0) (weightedAvgGlucose + 2.59) / 1.59 else 0.0
    }

    private fun calculateMetrics(entries: List<LogEntry>, smartAverages: Map<String, Float>): Quad<Double, Double, Int, Double> {
        val allRawValues = entries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
        val max = allRawValues.maxOrNull() ?: 0.0
        val simpleAvg = if (allRawValues.isNotEmpty()) allRawValues.average() else 0.0
        val highRate = if (allRawValues.isNotEmpty()) (allRawValues.count { it > 10.0 } * 100 / allRawValues.size) else 0

        val smartWeightedAvg = weightedAverage(smartAverages.values.toList())
        val hba1c = calculateHbA1c(smartWeightedAvg)

        return Quad(max, simpleAvg, highRate, hba1c)
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
        val outputSdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        
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

        val currentSmartAvgs = getSmartDailyAverages(currentEntries)
        val prevSmartAvgs = getSmartDailyAverages(previousEntries)

        val (max, avg, highRate, hba1c) = calculateMetrics(currentEntries, currentSmartAvgs)
        val (pMax, pAvg, pHighRate, pHba1c) = calculateMetrics(previousEntries, prevSmartAvgs)

        val currentPoints = currentSmartAvgs.toList().mapIndexed { index, pair -> 
            val dateLabel = try {
                val d = sdf.parse(pair.first)
                if (d != null) outputSdf.format(d) else pair.first
            } catch (e: Exception) { pair.first }
            ChartPointPro(index, pair.second.toDouble(), dateLabel)
        }

        val prevPoints = prevSmartAvgs.toList().mapIndexed { index, pair -> 
            val dateLabel = try {
                val d = sdf.parse(pair.first)
                if (d != null) outputSdf.format(d) else pair.first
            } catch (e: Exception) { pair.first }
            ChartPointPro(index, pair.second.toDouble(), dateLabel)
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
            insights = detectRisk.detect(currentEntries, currentSmartAvgs.values.toList()),
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
