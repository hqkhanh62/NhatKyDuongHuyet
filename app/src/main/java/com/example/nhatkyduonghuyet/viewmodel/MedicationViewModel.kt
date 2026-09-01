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
    val isTakenEvening: Boolean,
    val countThisMonth: Int
)

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

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
                        isTakenEvening = logsToday.any { it.session == "EVENING" },
                        countThisMonth = monthlyCount
                    )
                }
            }
            combine(flows) { it.toList() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleMedication(medId: Long, session: String, taken: Boolean) {
        viewModelScope.launch {
            repository.logMedication(medId, session, taken)
        }
    }

    fun importCsv(csvContent: String) {
        viewModelScope.launch {
            val lines = csvContent.lines()
            if (lines.isEmpty()) return@launch
            
            val newMeds = lines.drop(1) // Skip header
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.size >= 4) {
                        Medication(
                            name = parts[1].trim(),
                            dosage = parts[2].trim(),
                            instruction = parts[3].trim(),
                            timing = if (parts.size > 4) parts[4].trim() else ""
                        )
                    } else null
                }
            
            if (newMeds.isNotEmpty()) {
                repository.replaceMedications(newMeds)
            }
        }
    }

    fun prepopulateData() {
        viewModelScope.launch {
            repository.getAllMedications().first().let {
                if (it.isEmpty()) {
                    val initialMeds = listOf(
                        Medication(name = "Insulin Mixtard FlexPen", dosage = "100 IU/mL", instruction = "Trưa 6 đơn vị; chiều 8 đơn vị", timing = ""),
                        Medication(name = "Jardiance", dosage = "25 mg", instruction = "Sáng 1/2 viên", timing = ""),
                        Medication(name = "Clopistad", dosage = "75 mg", instruction = "Trưa 1 viên", timing = ""),
                        Medication(name = "Lipistad", dosage = "10 mg", instruction = "Chiều 1 viên", timing = ""),
                        Medication(name = "Valsartan", dosage = "80 mg", instruction = "Trưa 1/2 viên", timing = ""),
                        Medication(name = "Alfa-Lipogamma", dosage = "600 mg", instruction = "Sáng 1 viên", timing = "Sau thức ăn"),
                        Medication(name = "Trajenta", dosage = "5 mg", instruction = "Sáng 1/2 viên", timing = ""),
                        Medication(name = "Ketosteril", dosage = "", instruction = "Trưa 1 viên; chiều 1 viên", timing = "")
                    )
                    initialMeds.forEach { med -> repository.insertMedication(med) }
                }
            }
        }
    }
}
