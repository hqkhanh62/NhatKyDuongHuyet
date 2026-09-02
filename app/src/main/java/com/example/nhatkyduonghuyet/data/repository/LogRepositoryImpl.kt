package com.example.nhatkyduonghuyet.data.repository

import android.content.Context
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.model.AdvancedStatsEntity
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogRepositoryImpl @Inject constructor(
    private val dao: LogEntryDao,
    @ApplicationContext private val context: Context
) : LogRepository {
    override fun getAllLogs(): Flow<List<LogEntry>> = dao.getAllLogEntries()
    override fun getLogsByDate(date: String): Flow<List<LogEntry>> = dao.getEntriesForDate(date)
    
    override suspend fun insertLog(entry: LogEntry) {
        dao.upsert(entry)
        WidgetUpdater.requestUpdate(context)
    }

    override suspend fun updateLog(entry: LogEntry) {
        dao.update(entry)
        WidgetUpdater.requestUpdate(context)
    }

    override suspend fun deleteLog(entry: LogEntry) {
        dao.delete(entry)
        WidgetUpdater.requestUpdate(context)
    }

    override fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    override fun getAdvancedStats(): Flow<AdvancedStatsEntity> = dao.getAdvancedStats()
}
