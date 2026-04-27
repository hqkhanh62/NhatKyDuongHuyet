package com.example.nhatkyduonghuyet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries WHERE date = :date ORDER BY session")
    fun getEntriesForDate(date: String): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(logEntry: LogEntry)

    @Query("SELECT DISTINCT date FROM log_entries ORDER BY date DESC")
    fun getAllDates(): Flow<List<String>>

    @Query("SELECT * FROM log_entries")
    fun getAllLogEntries(): Flow<List<LogEntry>>
}
