package com.example.nhatkyduonghuyet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.model.AdvancedStatsEntity
import com.example.nhatkyduonghuyet.data.model.DailyAvgRow
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {

    @Query("SELECT * FROM log_entries WHERE date = :date")
    fun getByDate(date: String): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntry)

    @Upsert
    suspend fun upsert(entry: LogEntry)

    @Update
    suspend fun update(entry: LogEntry)

    @Delete
    suspend fun delete(entry: LogEntry)

    @Query("SELECT COUNT(*) FROM log_entries")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT * FROM log_entries WHERE date = :date")
    fun getEntriesForDate(date: String): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries ORDER BY date DESC, time DESC")
    fun getAllLogEntries(): Flow<List<LogEntry>>

    @Query("SELECT DISTINCT date FROM log_entries ORDER BY date DESC")
    fun getAllDates(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM log_entries WHERE (bgBefore IS NOT NULL OR bgAfter IS NOT NULL) AND session != 'AI Prediction'")
    fun getTotalValidMeasurementsCount(): Flow<Int>

    @Query("SELECT COUNT(*) as totalValid FROM log_entries WHERE (bgBefore IS NOT NULL OR bgAfter IS NOT NULL) AND session != 'AI Prediction'")
    fun getAdvancedStats(): Flow<AdvancedStatsEntity>

    @Query("""
        SELECT date, AVG(glucose) as averageValue
        FROM (
            SELECT date, bgBefore as glucose FROM log_entries WHERE bgBefore IS NOT NULL AND session != 'AI Prediction'
            UNION ALL
            SELECT date, bgAfter as glucose FROM log_entries WHERE bgAfter IS NOT NULL AND session != 'AI Prediction'
        )
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getDailyAverage(): Flow<List<DailyAvgRow>>
}
