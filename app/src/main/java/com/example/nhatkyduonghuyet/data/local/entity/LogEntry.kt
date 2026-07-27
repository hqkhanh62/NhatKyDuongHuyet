package com.example.nhatkyduonghuyet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val date: String,
    val session: String,
    val medType: String? = null,
    val dose: String? = null,
    val time: String? = null,
    val value: Int = 0,

    val bgBefore: Double? = null,
    val bgAfter: Double? = null,

    val bpSys: Int? = null,
    val bpDia: Int? = null,
    val heartRate: Int? = null,

    val note: String? = null
)
