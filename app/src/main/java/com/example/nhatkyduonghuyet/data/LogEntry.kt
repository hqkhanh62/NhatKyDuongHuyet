package com.example.nhatkyduonghuyet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val session: String,
    val medType: String?,
    val dose: String?,
    val time: String?,
    val bgBefore: Double?,
    val bgAfter: Double?,
    val note: String?
)
