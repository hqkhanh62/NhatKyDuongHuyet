package com.example.nhatkyduonghuyet.data.repository

import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogRepository(
    private val dao: LogEntryDao
) {
    fun getTotalCount() = dao.getAdvancedStats().map { it.totalValid }

    fun getAdvancedStats() = dao.getAdvancedStats()

    fun getDailyAverage() = dao.getDailyAverage()

    fun getAllEntries() = dao.getAllLogEntries()

    fun getEntries(date: String): Flow<List<LogEntry>> = dao.getEntriesForDate(date)

    suspend fun update(entry: LogEntry) = dao.update(entry)

    suspend fun upsert(entry: LogEntry) = dao.upsert(entry)
}
