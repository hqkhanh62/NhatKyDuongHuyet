package com.example.nhatkyduonghuyet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogEntryViewModel @Inject constructor(
    private val repository: LogRepository
) : ViewModel() {

    private val _currentDate = MutableStateFlow(getCurrentDateFormatted())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val allLogEntries: StateFlow<List<LogEntry>> = repository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDates: StateFlow<List<String>> = repository.getAllLogs()
        .map { logs -> logs.map { it.date }.distinct().sortedDescending() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val entriesForSelectedDate: StateFlow<List<LogEntry>> = _currentDate
        .flatMapLatest { date -> repository.getLogsByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getAllLogEntries() = repository.getAllLogs()

    fun selectDate(date: String) {
        _currentDate.value = date
    }

    fun upsertLogEntry(logEntry: LogEntry) {
        viewModelScope.launch {
            repository.insertLog(logEntry)
        }
    }

    fun importLogEntries(entries: List<LogEntry>) {
        viewModelScope.launch {
            entries.forEach { repository.insertLog(it) }
        }
    }

    private fun getCurrentDateFormatted(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun getDailyChartData(date: String): StateFlow<List<Pair<String, Double>>> {
        val dailyData = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
        viewModelScope.launch {
            repository.getLogsByDate(date).collect { entries ->
                val dataPoints = mutableListOf<Pair<String, Double>>()
                entries.forEach { entry ->
                    entry.time?.let { time ->
                        entry.bgBefore?.let { bg -> dataPoints.add(time to bg) }
                        entry.bgAfter?.let { bg -> dataPoints.add(time to bg) }
                    }
                }
                dailyData.value = dataPoints.sortedBy { it.first }
            }
        }
        return dailyData
    }
}
