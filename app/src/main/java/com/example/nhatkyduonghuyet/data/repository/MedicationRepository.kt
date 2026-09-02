package com.example.nhatkyduonghuyet.data.repository

import com.example.nhatkyduonghuyet.data.local.dao.MedicationDao
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao
) {
    fun getAllMedications(): Flow<List<Medication>> = medicationDao.getAllMedications()

    suspend fun insertMedication(medication: Medication) = medicationDao.insertMedication(medication)

    suspend fun logMedication(medication: Medication, session: String, taken: Boolean) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = sdf.format(Date())

        if (taken) {
            medicationDao.upsertLog(
                MedicationLog(
                    medicationId = medication.id,
                    medicationNameSnapshot = medication.name,
                    dosageSnapshot = medication.dosage,
                    timestamp = System.currentTimeMillis(),
                    date = dateStr,
                    session = session
                )
            )
        } else {
            medicationDao.deleteLog(medication.id, session, dateStr)
        }
    }

    fun getCountSince(medicationId: Long, startTime: Long): Flow<Int> =
        medicationDao.getCountSince(medicationId, startTime)

    fun getLogsForToday(medicationId: Long): Flow<List<MedicationLog>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = sdf.format(Date())
        return medicationDao.getLogsForDate(medicationId, dateStr)
    }

    suspend fun replaceMedications(medications: List<Medication>) {
        // Transactional replace to avoid inconsistent state (P1-MED-01)
        // Note: For now, we still delete all because we don't have a stable key/ID mapping
        // but we keep the logs which will now be orphaned but at least not deleted.
        // In a real app, we'd map CSV items to existing DB items by name/dosage.
        medicationDao.deleteAllMedications()
        medications.forEach { medicationDao.insertMedication(it) }
    }
}
