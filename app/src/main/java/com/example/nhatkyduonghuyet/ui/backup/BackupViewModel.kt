package com.example.nhatkyduonghuyet.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.backup.BackupPart
import com.example.nhatkyduonghuyet.data.backup.AutoExportWorker
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
    val isBusy: Boolean = false,
    val autoExportEnabled: Boolean = false,
    val autoExportFolderName: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val appContext: android.content.Context
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
        refreshAutoExportState()
    }

    private fun refreshAutoExportState() {
        _uiState.value = _uiState.value.copy(
            autoExportEnabled = repository.isAutoExportEnabled,
            autoExportFolderName = repository.autoExportFolder?.lastPathSegment
        )
    }

    /** User picked a folder for the weekly automatic export. */
    fun enableAutoExport(folder: Uri) {
        viewModelScope.launch {
            val ok = repository.enableAutoExport(folder)
            _message.value = if (ok) {
                AutoExportWorker.schedule(appContext)
                "Đã bật xuất tự động hằng tuần vào thư mục đã chọn."
            } else {
                "Không giữ được quyền ghi vào thư mục này. Hãy thử thư mục khác."
            }
            refreshAutoExportState()
        }
    }

    fun disableAutoExport() {
        repository.disableAutoExport()
        AutoExportWorker.cancel(appContext)
        _message.value = "Đã tắt xuất tự động."
        refreshAutoExportState()
    }

    fun exportBundle(uri: Uri) = launchAction { repository.exportBundle(uri) }

    fun bundleFileName(): String = repository.suggestedBundleName()

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
            refreshAutoExportState()
        }
    }
}
