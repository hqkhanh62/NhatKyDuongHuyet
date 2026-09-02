package com.example.nhatkyduonghuyet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_logs",
    indices = [Index(value = ["medicationId", "date", "session"], unique = true)]
)
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationNameSnapshot: String, // MED-01 fix: keep name snapshot
    val dosageSnapshot: String, // MED-01 fix: keep dosage snapshot
    val timestamp: Long,
    val date: String, // yyyy-MM-dd for unique constraint
    val session: String, // "MORNING", "NOON", "AFTERNOON", "EVENING", "BEDTIME"
    val amountTaken: Float = 1.0f
)
