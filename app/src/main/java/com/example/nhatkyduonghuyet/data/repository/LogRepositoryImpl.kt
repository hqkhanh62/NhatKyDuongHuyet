package com.example.nhatkyduonghuyet.data.repository

import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogRepositoryImpl @Inject constructor(
    private val dao: LogEntryDao
) : LogRepository {
    override fun getAllLogs(): Flow<List<LogEntry>> = dao.getAllLogEntries()
    override fun getLogsByDate(date: String): Flow<List<LogEntry>> = dao.getEntriesForDate(date)
    override suspend fun insertLog(entry: LogEntry) = dao.upsert(entry)
    override suspend fun updateLog(entry: LogEntry) = dao.update(entry)
    override suspend fun deleteLog(entry: LogEntry) = dao.delete(entry)
    override fun getTotalCount(): Flow<Int> = dao.getTotalCount()
}
