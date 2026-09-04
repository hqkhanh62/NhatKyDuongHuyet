package com.example.nhatkyduonghuyet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.local.entity.Medication
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
    private val repository: MedicationRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    /** One-shot message for the snackbar; call [consumeMessage] after showing. */
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
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
                _message.value = "File quá lớn (giới hạn 100KB)."
                return@launch
            }

            val newMeds = com.example.nhatkyduonghuyet.util.MedicationCsv
                .parsePrescriptionCsv(csvContent)

            if (newMeds.isNotEmpty()) {
                repository.replaceMedications(newMeds)
                _message.value = "Đã nhập ${newMeds.size} thuốc từ file CSV."
            } else {
                _message.value = "File không có dòng thuốc hợp lệ."
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
