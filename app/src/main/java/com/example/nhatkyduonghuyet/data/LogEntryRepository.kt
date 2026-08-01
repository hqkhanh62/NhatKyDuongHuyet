package com.example.nhatkyduonghuyet.data

import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogEntryRepository @Inject constructor(private val logEntryDao: LogEntryDao) {
    fun getEntriesForDate(date: String): Flow<List<LogEntry>> = logEntryDao.getEntriesForDate(date)

    suspend fun upsert(logEntry: LogEntry) {
        logEntryDao.upsert(logEntry)
    }

    fun getAllDates(): Flow<List<String>> = logEntryDao.getAllDates()

    fun getAllLogEntries(): Flow<List<LogEntry>> = logEntryDao.getAllLogEntries()
}
