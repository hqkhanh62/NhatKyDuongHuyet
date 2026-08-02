package com.example.nhatkyduonghuyet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: LogRepository
) : ViewModel() {

    private val _date = MutableStateFlow("2026-01-01")

    val entries: StateFlow<List<LogEntry>> =
        _date.flatMapLatest { repo.getLogsByDate(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun onEntryChanged(entry: LogEntry) {
        viewModelScope.launch {
            repo.updateLog(entry) // 🔥 auto save
        }
    }
}
