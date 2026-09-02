package com.example.nhatkyduonghuyet.data.local.dao

import androidx.room.*
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MedicationDao {
    @Query("SELECT * FROM medications")
    abstract fun getAllMedications(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMedication(medication: Medication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertLog(log: MedicationLog)

    @Query("SELECT COUNT(*) FROM medication_logs WHERE medicationId = :medicationId AND timestamp >= :startTime")
    abstract fun getCountSince(medicationId: Long, startTime: Long): Flow<Int>

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId AND date = :date")
    abstract fun getLogsForDate(medicationId: Long, date: String): Flow<List<MedicationLog>>

    @Query("DELETE FROM medication_logs WHERE medicationId = :medicationId AND session = :session AND date = :date")
    abstract suspend fun deleteLog(medicationId: Long, session: String, date: String)

    @Query("DELETE FROM medications")
    abstract suspend fun deleteAllMedications()

    @Transaction
    open suspend fun replaceAllMedications(meds: List<Medication>) {
        deleteAllMedications()
        meds.forEach { insertMedication(it) }
    }
}
