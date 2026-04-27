package com.example.nhatkyduonghuyet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.LogEntry
import com.example.nhatkyduonghuyet.data.LogEntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogEntryViewModel(private val repository: LogEntryRepository) : ViewModel() {

    private val _allDates = MutableStateFlow<List<String>>(emptyList())
    val allDates: StateFlow<List<String>> = _allDates.asStateFlow()

    private val _entriesForSelectedDate = MutableStateFlow<List<LogEntry>>(emptyList())
    val entriesForSelectedDate: StateFlow<List<LogEntry>> = _entriesForSelectedDate.asStateFlow()

    private val _currentDate = MutableStateFlow(getCurrentDateFormatted())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    fun getAllLogEntries(): Flow<List<LogEntry>> = repository.getAllLogEntries()

    init {
        viewModelScope.launch {
            repository.getAllDates().collect { dates ->
                _allDates.value = dates
            }
        }
        viewModelScope.launch {
            _currentDate.collect { date ->
                repository.getEntriesForDate(date).collect { entries ->
                    _entriesForSelectedDate.value = entries
                }
            }
        }
    }

    fun selectDate(date: String) {
        _currentDate.value = date
    }

    fun upsertLogEntry(logEntry: LogEntry) {
        viewModelScope.launch {
            repository.upsert(logEntry)
        }
    }

    private fun getCurrentDateFormatted(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    // Chart data preparation
    fun getDailyChartData(date: String): StateFlow<List<Pair<String, Double>>> {
        val dailyData = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
        viewModelScope.launch {
            repository.getEntriesForDate(date).collect { entries ->
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

    fun getWeeklyChartData(): StateFlow<List<Pair<String, Double>>> {
        val weeklyData = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
        viewModelScope.launch {
            repository.getAllLogEntries().collect { allEntries ->
                val groupedByDate = allEntries.groupBy { it.date }
                val aggregatedData = mutableListOf<Pair<String, Double>>()

                groupedByDate.forEach { (date, entries) ->
                    val totalBg = entries.sumOf { (it.bgBefore ?: 0.0) + (it.bgAfter ?: 0.0) }
                    val count = entries.count { it.bgBefore != null } + entries.count { it.bgAfter != null }
                    if (count > 0) {
                        aggregatedData.add(date to (totalBg / count))
                    }
                }
                weeklyData.value = aggregatedData.sortedBy { it.first }
            }
        }
        return weeklyData
    }

    companion object {
        fun provideFactory(repository: LogEntryRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LogEntryViewModel(repository) as T
            }
        }
    }
}
