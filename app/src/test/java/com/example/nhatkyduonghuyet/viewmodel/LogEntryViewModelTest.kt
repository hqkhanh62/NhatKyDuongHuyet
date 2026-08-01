package com.example.nhatkyduonghuyet.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.nhatkyduonghuyet.data.LogEntry
import com.example.nhatkyduonghuyet.data.LogEntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Assert.assertEquals

@ExperimentalCoroutinesApi
class LogEntryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    @Mock
    private lateinit var repository: LogEntryRepository

    private lateinit var viewModel: LogEntryViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = LogEntryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `upsertLogEntry calls repository upsert`() = testDispatcher.runBlockingTest {
        val logEntry = LogEntry(date = "2026-04-26", session = "Sáng", bgBefore = 5.5)
        viewModel.upsertLogEntry(logEntry)
        verify(repository).upsert(logEntry)
    }

    @Test
    fun `allDates are collected from repository`() = testDispatcher.runBlockingTest {
        val dates = listOf("2026-04-26", "2026-04-25")
        whenever(repository.getAllDates()).thenReturn(flowOf(dates))

        viewModel = LogEntryViewModel(repository) // Re-initialize to trigger init block

        assertEquals(dates, viewModel.allDates.value)
    }

    @Test
    fun `entriesForSelectedDate are collected from repository`() = testDispatcher.runBlockingTest {
        val date = "2026-04-26"
        val entries = listOf(LogEntry(date = date, session = "Sáng", bgBefore = 5.5))
        whenever(repository.getEntriesForDate(date)).thenReturn(flowOf(entries))

        viewModel.selectDate(date)
        testDispatcher.advanceUntilIdle() // Allow coroutines to complete

        assertEquals(entries, viewModel.entriesForSelectedDate.value)
    }

    @Test
    fun `getAllLogEntries calls repository getAllLogEntries`() = testDispatcher.runBlockingTest {
        val entries = listOf(LogEntry(date = "2026-04-26", session = "Sáng", bgBefore = 5.5))
        whenever(repository.getAllLogEntries()).thenReturn(flowOf(entries))

        val result = viewModel.getAllLogEntries()
        assertEquals(entries, result.value)
    }
}
