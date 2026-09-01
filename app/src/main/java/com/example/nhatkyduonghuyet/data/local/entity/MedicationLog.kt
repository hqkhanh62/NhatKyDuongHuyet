package com.example.nhatkyduonghuyet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val timestamp: Long,
    val session: String = "ALL", // "MORNING", "NOON", "EVENING", "ALL"
    val amountTaken: Float = 1.0f
)
