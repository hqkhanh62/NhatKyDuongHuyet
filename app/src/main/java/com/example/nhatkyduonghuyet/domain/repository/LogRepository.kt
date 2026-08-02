package com.example.nhatkyduonghuyet.domain.repository

import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.model.AdvancedStatsEntity
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getAllLogs(): Flow<List<LogEntry>>
    fun getLogsByDate(date: String): Flow<List<LogEntry>>
    suspend fun insertLog(entry: LogEntry)
    suspend fun updateLog(entry: LogEntry)
    suspend fun deleteLog(entry: LogEntry)
    fun getTotalCount(): Flow<Int>
    fun getAdvancedStats(): Flow<AdvancedStatsEntity>
}
