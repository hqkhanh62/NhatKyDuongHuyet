package com.example.nhatkyduonghuyet.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LogEntryDaoTest {

    private lateinit var logEntryDao: LogEntryDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        logEntryDao = db.logEntryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun upsertAndGetEntriesForDate() = runBlocking {
        val entry1 = LogEntry(date = "2026-04-26", session = "Sáng", bgBefore = 5.5)
        val entry2 = LogEntry(date = "2026-04-26", session = "Trưa", bgAfter = 7.2)
        val entry3 = LogEntry(date = "2026-04-27", session = "Sáng", bgBefore = 6.0)

        logEntryDao.upsert(entry1)
        logEntryDao.upsert(entry2)
        logEntryDao.upsert(entry3)

        val entriesForToday = logEntryDao.getEntriesForDate("2026-04-26").first()
        assert(entriesForToday.size == 2)
        assert(entriesForToday[0].session == "Sáng")
        assert(entriesForToday[1].session == "Trưa")

        val entriesForTomorrow = logEntryDao.getEntriesForDate("2026-04-27").first()
        assert(entriesForTomorrow.size == 1)
        assert(entriesForTomorrow[0].session == "Sáng")
    }

    @Test
    @Throws(Exception::class)
    fun getAllDates() = runBlocking {
        val entry1 = LogEntry(date = "2026-04-26", session = "Sáng", bgBefore = 5.5)
        val entry2 = LogEntry(date = "2026-04-27", session = "Trưa", bgAfter = 7.2)
        val entry3 = LogEntry(date = "2026-04-25", session = "Tối", bgBefore = 6.0)

        logEntryDao.upsert(entry1)
        logEntryDao.upsert(entry2)
        logEntryDao.upsert(entry3)

        val allDates = logEntryDao.getAllDates().first()
        assert(allDates.size == 3)
        assert(allDates[0] == "2026-04-27") // Should be sorted descending
        assert(allDates[1] == "2026-04-26")
        assert(allDates[2] == "2026-04-25")
    }

    @Test
    @Throws(Exception::class)
    fun upsertReplacesExisting() = runBlocking {
        val entry1 = LogEntry(date = "2026-04-26", session = "Sáng", bgBefore = 5.5)
        logEntryDao.upsert(entry1)

        val updatedEntry = entry1.copy(bgBefore = 6.0)
        logEntryDao.upsert(updatedEntry)

        val entries = logEntryDao.getEntriesForDate("2026-04-26").first()
        assert(entries.size == 1)
        assert(entries[0].bgBefore == 6.0)
    }
}
