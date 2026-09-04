package com.example.nhatkyduonghuyet.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.dao.MedicationDao
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog

/**
 * Schema export is ON: `app/schemas/*.json` is committed so every future
 * version bump can be diffed and covered by a migration test. Without it the
 * only safe option was a destructive rebuild, which is what used to wipe the
 * diary and the medication history on each app update.
 */
@Database(
    entities = [LogEntry::class, Medication::class, MedicationLog::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logEntryDao(): LogEntryDao
    abstract fun medicationDao(): MedicationDao
}