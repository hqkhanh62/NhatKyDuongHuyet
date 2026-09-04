package com.example.nhatkyduonghuyet.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.backup.BackupPart
import com.example.nhatkyduonghuyet.data.backup.BackupRepository
import com.example.nhatkyduonghuyet.data.backup.BackupResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val diaryCount: Int = 0,
    val medicationCount: Int = 0,
    val medicationLogCount: Int = 0,
    val isBusy: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val lastBackupAt: StateFlow<Long> = repository.lastBackupAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Null = never exported. Used to nag the user before an uninstall. */
    val daysSinceExport: StateFlow<Long?> = repository.daysSinceExport
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        refreshCounts()
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun refreshCounts() {
        viewModelScope.launch {
            val snapshot = repository.snapshot()
            _uiState.value = _uiState.value.copy(
                diaryCount = snapshot.logEntries.size,
                medicationCount = snapshot.medications.size,
                medicationLogCount = snapshot.medicationLogs.size
            )
        }
    }

    fun fileNameFor(part: BackupPart): String = repository.suggestedFileName(part)

    fun export(part: BackupPart, uri: Uri) = launchAction { repository.export(part, uri) }

    fun restore(uri: Uri, displayName: String?) =
        launchAction { repository.restoreFromUri(uri, displayName) }

    fun backupNow() = launchAction { repository.backupNow() }

    fun restoreFromRollingSnapshot() =
        launchAction { repository.restoreFromRollingSnapshot() }

    private fun launchAction(action: suspend () -> BackupResult) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val result = action()
            _message.value = when (result) {
                is BackupResult.Success -> result.message
                is BackupResult.Failure -> result.message
            }
            _uiState.value = _uiState.value.copy(isBusy = false)
            refreshCounts()
        }
    }
}
