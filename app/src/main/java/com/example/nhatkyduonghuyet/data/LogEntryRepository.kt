package com.example.nhatkyduonghuyet.data

import kotlinx.coroutines.flow.Flow

class LogEntryRepository(private val logEntryDao: LogEntryDao) {
    fun getEntriesForDate(date: String): Flow<List<LogEntry>> = logEntryDao.getEntriesForDate(date)

    suspend fun upsert(logEntry: LogEntry) {
        logEntryDao.upsert(logEntry)
    }

    fun getAllDates(): Flow<List<String>> = logEntryDao.getAllDates()

    fun getAllLogEntries(): Flow<List<LogEntry>> = logEntryDao.getAllLogEntries()
}
