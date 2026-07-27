package com.example.nhatkyduonghuyet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.repository.LogRepository
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class TimeFilter(val days: Int, val label: String) {
    LAST_7_DAYS(7, "7 ngày qua"),
    LAST_15_DAYS(15, "15 ngày qua"),
    LAST_30_DAYS(30, "30 ngày qua"),
    ALL(Int.MAX_VALUE, "Tất cả")
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repo: LogRepository
) : ViewModel() {

    private val _timeFilter = MutableStateFlow(TimeFilter.LAST_7_DAYS)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter.asStateFlow()

    // Visibility states for chart lines
    private val _showBefore = MutableStateFlow(true)
    val showBefore = _showBefore.asStateFlow()

    private val _showAfter = MutableStateFlow(true)
    val showAfter = _showAfter.asStateFlow()

    private val _showDaily = MutableStateFlow(false)
    val showDaily = _showDaily.asStateFlow()

    private val allEntries = repo.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val filteredEntries: StateFlow<List<LogEntry>> = combine(allEntries, _timeFilter) { entries, filter ->
        if (filter == TimeFilter.ALL) return@combine entries
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -filter.days)
        val limitDate = calendar.time
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        entries.filter {
            try {
                val entryDate = sdf.parse(it.date)
                entryDate != null && entryDate.after(limitDate)
            } catch (e: Exception) {
                false
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val totalCount = filteredEntries.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val stats = filteredEntries.map { list ->
        val values = list.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }

        val avg = if (values.isEmpty()) 0.0 else values.average()
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0

        val lowCount = values.count { it < 4.0 }
        val normalCount = values.count { it in 4.0..7.0 }
        val preCount = values.count { it > 7.0 && it <= 11.0 }
        val highCount = values.count { it > 11.0 }

        val total = values.size.toDouble()

        val hba1c = if (avg > 0) (avg + 2.59) / 1.59 else 0.0

        StatsUi(
            avg = avg,
            min = min,
            max = max,
            hba1c = hba1c,
            lowCount = lowCount,
            normalCount = normalCount,
            preCount = preCount,
            highCount = highCount,
            lowPercent = if (total > 0) (lowCount * 100 / total).toInt() else 0,
            normalPercent = if (total > 0) (normalCount * 100 / total).toInt() else 0,
            prePercent = if (total > 0) (preCount * 100 / total).toInt() else 0,
            highPercent = if (total > 0) (highCount * 100 / total).toInt() else 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), StatsUi())

    val chartData: StateFlow<List<MultiSeriesPoint>> = filteredEntries.map { entries ->
        val groupedByDate = entries.groupBy { it.date }.toSortedMap()
        groupedByDate.map { (date, list) ->
            val avgBefore = list.mapNotNull { it.bgBefore }.let { if (it.isEmpty()) null else it.average() }
            val avgAfter = list.mapNotNull { it.bgAfter }.let { if (it.isEmpty()) null else it.average() }
            val avgDaily = list.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }.let { if (it.isEmpty()) null else it.average() }
            MultiSeriesPoint(
                date = date,
                avgBefore = avgBefore,
                avgAfter = avgAfter,
                avgDaily = avgDaily
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    fun toggleBefore() { _showBefore.value = !_showBefore.value }
    fun toggleAfter() { _showAfter.value = !_showAfter.value }
    fun toggleDaily() { _showDaily.value = !_showDaily.value }
}

data class StatsUi(
    val avg: Double = 0.0,
    val min: Double = 0.0,
    val max: Double = 0.0,
    val hba1c: Double = 0.0,
    val lowCount: Int = 0,
    val normalCount: Int = 0,
    val preCount: Int = 0,
    val highCount: Int = 0,
    val lowPercent: Int = 0,
    val normalPercent: Int = 0,
    val prePercent: Int = 0,
    val highPercent: Int = 0
)

data class MultiSeriesPoint(
    val date: String,
    val avgBefore: Double?,
    val avgAfter: Double?,
    val avgDaily: Double?
)
