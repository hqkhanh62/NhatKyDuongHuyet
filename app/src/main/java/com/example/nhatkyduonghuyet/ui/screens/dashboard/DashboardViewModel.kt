package com.example.nhatkyduonghuyet.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.domain.usecase.PredictGlucose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: LogRepository,
    private val predictGlucose: PredictGlucose
) : ViewModel() {

    // Luồng dữ liệu nhật ký từ Database
    val logs: StateFlow<List<LogEntry>> = repository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Luồng dự báo AI dựa trên LSTM
    val prediction: StateFlow<PredictionResult?> = logs.map { 
        predictGlucose(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
