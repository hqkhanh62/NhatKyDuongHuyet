package com.example.nhatkyduonghuyet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import android.net.Uri
import com.example.nhatkyduonghuyet.data.repository.BackupOutcome
import com.example.nhatkyduonghuyet.data.repository.MedicationBackupRepository
import com.example.nhatkyduonghuyet.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class MedicationUiState(
    val medication: Medication,
    val isTakenMorning: Boolean,
    val isTakenNoon: Boolean,
    val isTakenAfternoon: Boolean,
    val isTakenEvening: Boolean,
    val isTakenBedtime: Boolean,
    val countThisMonth: Int
)

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val backupRepository: MedicationBackupRepository
) : ViewModel() {

    private val _backupMessage = MutableStateFlow<String?>(null)
    /** One-shot message for the snackbar; call [consumeBackupMessage] after showing. */
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    val lastBackupAt: StateFlow<Long> = backupRepository.lastBackupAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun consumeBackupMessage() {
        _backupMessage.value = null
    }

    private fun report(outcome: BackupOutcome) {
        _backupMessage.value = when (outcome) {
            is BackupOutcome.Success -> outcome.message
            is BackupOutcome.Failure -> outcome.message
        }
    }

    /** Suggested filenames for the SAF create-document dialogs. */
    fun prescriptionFileName(): String =
        "don_thuoc_" + fileStamp() + ".csv"

    fun historyFileName(): String =
        "lich_su_uong_thuoc_" + fileStamp() + ".csv"

    private fun fileStamp(): String =
        java.text.SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

    fun exportPrescription(uri: Uri) {
        viewModelScope.launch { report(backupRepository.exportPrescription(uri)) }
    }

    fun exportHistory(uri: Uri) {
        viewModelScope.launch { report(backupRepository.exportHistory(uri)) }
    }

    fun backupNow() {
        viewModelScope.launch { report(backupRepository.backupNow()) }
    }

    fun restoreFromBackup() {
        viewModelScope.launch { report(backupRepository.restoreFromLatestSnapshot()) }
    }

    fun importPrescription(uri: Uri) {
        viewModelScope.launch { report(backupRepository.importPrescriptionFromUri(uri)) }
    }

    val medicationList: Flow<List<MedicationUiState>> = repository.getAllMedications().flatMapLatest { meds ->
        if (meds.isEmpty()) {
            flowOf(emptyList())
        } else {
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val flows = meds.map { med ->
                combine(
                    repository.getLogsForToday(med.id),
                    repository.getCountSince(med.id, startOfMonth)
                ) { logsToday, monthlyCount ->
                    MedicationUiState(
                        medication = med,
                        isTakenMorning = logsToday.any { it.session == "MORNING" },
                        isTakenNoon = logsToday.any { it.session == "NOON" },
                        isTakenAfternoon = logsToday.any { it.session == "AFTERNOON" },
                        isTakenEvening = logsToday.any { it.session == "EVENING" },
                        isTakenBedtime = logsToday.any { it.session == "BEDTIME" },
                        countThisMonth = monthlyCount
                    )
                }
            }
            combine(flows) { it.toList() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleMedication(med: Medication, session: String, taken: Boolean) {
        viewModelScope.launch {
            repository.logMedication(med, session, taken)
        }
    }

    fun importCsv(csvContent: String) {
        viewModelScope.launch {
            // Limit CSV content size to prevent memory issues (MED-04)
            if (csvContent.length > 100_000) {
                _backupMessage.value = "File quá lớn (giới hạn 100KB)."
                return@launch
            }

            val newMeds = com.example.nhatkyduonghuyet.util.MedicationCsv
                .parsePrescriptionCsv(csvContent)

            if (newMeds.isNotEmpty()) {
                repository.replaceMedications(newMeds)
                _backupMessage.value = "Đã nhập ${newMeds.size} thuốc từ file CSV."
            } else {
                _backupMessage.value = "File không có dòng thuốc hợp lệ."
            }
        }
    }

    fun prepopulateData() {
        viewModelScope.launch {
            repository.getAllMedications().first().let {
                if (it.isEmpty()) {
                    val initialMeds = listOf(
                        Medication(name = "Insulin Mixtard FlexPen", dosage = "100 IU/mL", instruction = "Trưa 6 đơn vị; chiều 8 đơn vị", timing = ""),
                        Medication(name = "Jardiance", dosage = "25 mg", instruction = "Sáng 1/2 v", timing = ""),
                        Medication(name = "Clopistad", dosage = "75 mg", instruction = "Trưa 1 v", timing = ""),
                        Medication(name = "Lipistad", dosage = "10 mg", instruction = "Chiều 1 v", timing = ""),
                        Medication(name = "Valsartan", dosage = "80 mg", instruction = "Trưa 1/2 v", timing = ""),
                        Medication(name = "Alfa-Lipogamma", dosage = "600 mg", instruction = "Sáng 1 v", timing = ""),
                        Medication(name = "Trajenta", dosage = "5 mg", instruction = "Sáng 1/2 v", timing = ""),
                        Medication(name = "Ketosteril", dosage = "", instruction = "Trưa 1 v; chiều 1 v", timing = "")
                    )
                    initialMeds.forEach { med -> repository.insertMedication(med) }
                }
            }
        }
    }
}
