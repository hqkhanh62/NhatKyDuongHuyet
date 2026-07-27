package com.example.nhatkyduonghuyet

import android.content.Context
import com.example.nhatkyduonghuyet.data.LogEntryRepository
import com.example.nhatkyduonghuyet.data.local.AppDatabase

interface AppContainer {
    val logEntryRepository: LogEntryRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val logEntryRepository: LogEntryRepository by lazy {
        LogEntryRepository(AppDatabase.getDatabase(context).logEntryDao())
    }
}
