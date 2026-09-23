package com.example.nhatkyduonghuyet.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nhatkyduonghuyet.data.local.dao.LogEntryDao
import com.example.nhatkyduonghuyet.data.local.dao.MedicationDao
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.data.local.entity.Medication
import com.example.nhatkyduonghuyet.data.local.entity.MedicationLog

/**
 * NOTE: `exportSchema` stays false for now - enabling it needs a kapt/AGP
 * config change that currently breaks the build. The migrations in
 * [DatabaseMigrations] are written defensively so they do not depend on the
 * exported schema JSON, and they are covered by JVM unit tests.
 */
@Database(
    entities = [LogEntry::class, Medication::class, MedicationLog::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logEntryDao(): LogEntryDao
    abstract fun medicationDao(): MedicationDao
}