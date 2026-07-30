package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.repository.LogRepository
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val entries: List<LogEntry> = emptyList(),
    val max: Double = 0.0,
    val avg: Double = 0.0,
    val highRate: Int = 0,
    val insights: List<String> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repo: LogRepository,
    private val detectRisk: DetectRiskPattern
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = repo.getAllEntries()
        .map { entries ->
            val values = entries.mapNotNull { it.bgAfter ?: it.bgBefore }

            val max = values.maxOrNull() ?: 0.0
            val avg = if (values.isNotEmpty()) values.average() else 0.0

            val highRate = if (values.isNotEmpty())
                (values.count { it > 10.0 } * 100 / values.size)
            else 0

            DashboardUiState(
                entries = entries,
                max = max,
                avg = avg,
                highRate = highRate,
                insights = detectRisk(entries)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )
}
