package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.repository.AIRepository
import com.example.nhatkyduonghuyet.data.repository.LogRepository
import com.example.nhatkyduonghuyet.domain.usecase.DetectRiskPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardAiState(
    val morningPrediction: Float = 0f,
    val morningRisk: String = "Đang tính...",
    val afternoonPrediction: Float = 0f,
    val afternoonRisk: String = "Đang tính..."
)

data class DashboardUiState(
    val entries: List<LogEntry> = emptyList(),
    val max: Double = 0.0,
    val avg: Double = 0.0,
    val highRate: Int = 0,
    val insights: List<String> = emptyList(),
    val ai: DashboardAiState = DashboardAiState()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: LogRepository,
    private val aiRepo: AIRepository,
    private val detectRisk: DetectRiskPattern
) : ViewModel() {

    private val _aiState = MutableStateFlow(DashboardAiState())

    init {
        // Start AI Update loop
        viewModelScope.launch {
            while (true) {
                val morningInput = floatArrayOf(5.0f, 5.3f, 5.8f, 6.1f, 5.7f)
                val afternoonInput = floatArrayOf(7.8f, 8.2f, 9.1f, 8.5f, 8.0f)
                
                val mResult = aiRepo.runPrediction(morningInput)
                val aResult = aiRepo.runPrediction(afternoonInput)
                
                _aiState.value = DashboardAiState(
                    morningPrediction = mResult.value,
                    morningRisk = mResult.risk,
                    afternoonPrediction = aResult.value,
                    afternoonRisk = aResult.risk
                )
                delay(5000)
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(repo.getAllEntries(), _aiState) { entries, ai ->
        val values = entries.mapNotNull { it.bgAfter ?: it.bgBefore }
        val max = values.maxOrNull() ?: 0.0
        val avg = if (values.isNotEmpty()) values.average() else 0.0
        val highRate = if (values.isNotEmpty()) (values.count { it > 10.0 } * 100 / values.size) else 0

        DashboardUiState(
            entries = entries,
            max = max,
            avg = avg,
            highRate = highRate,
            insights = detectRisk(entries),
            ai = ai
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
