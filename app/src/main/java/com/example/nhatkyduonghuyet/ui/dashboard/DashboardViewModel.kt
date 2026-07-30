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

data class DashboardUiState(
    val entries: List<LogEntry> = emptyList(),
    val max: Double = 0.0,
    val avg: Double = 0.0,
    val highRate: Int = 0,
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

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getAllEntries(),
        _timeFilter
    ) { allEntries, filter ->
        val filteredEntries = if (filter == DashboardTimeFilter.ALL) {
            allEntries
        } else {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -filter.days)
            val limitDate = calendar.time
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            allEntries.filter {
                try {
                    val entryDate = sdf.parse(it.date)
                    entryDate != null && (entryDate.after(limitDate) || it.date == sdf.format(Date()))
                } catch (e: Exception) {
                    false
                }
            }
        }

        val values = filteredEntries.mapNotNull { it.bgAfter ?: it.bgBefore }

        val max = values.maxOrNull() ?: 0.0
        val avg = if (values.isNotEmpty()) values.average() else 0.0

        val highRate = if (values.isNotEmpty())
            (values.count { it > 10.0 } * 100 / values.size)
        else 0

        DashboardUiState(
            entries = filteredEntries,
            max = max,
            avg = avg,
            highRate = highRate,
            insights = detectRisk(filteredEntries),
            currentFilter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
