package com.example.nhatkyduonghuyet.data.repository

import com.example.nhatkyduonghuyet.data.local.dao.MedicationDao
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao
) {
    fun getAllMedications(): Flow<List<Medication>> = medicationDao.getAllMedications()

    suspend fun insertMedication(medication: Medication) = medicationDao.insertMedication(medication)

    suspend fun logMedication(medicationId: Long, session: String, taken: Boolean) {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (taken) {
            medicationDao.insertLog(MedicationLog(
                medicationId = medicationId, 
                timestamp = System.currentTimeMillis(),
                session = session
            ))
        } else {
            medicationDao.deleteLog(medicationId, session, startOfDay)
        }
    }

    fun getCountSince(medicationId: Long, startTime: Long): Flow<Int> =
        medicationDao.getCountSince(medicationId, startTime)

    fun getLogsForToday(medicationId: Long): Flow<List<MedicationLog>> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return medicationDao.getLogsForToday(medicationId, startOfDay)
    }

    suspend fun replaceMedications(medications: List<Medication>) {
        medicationDao.deleteAllMedications()
        medications.forEach { medicationDao.insertMedication(it) }
    }
}
