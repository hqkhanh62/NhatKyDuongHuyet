package com.example.nhatkyduonghuyet.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Assert.assertEquals

import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class LogEntryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var repository: LogRepository

    private lateinit var viewModel: LogEntryViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock default flows to avoid null errors during init
        whenever(repository.getAllLogs()).thenReturn(flowOf(emptyList()))
        whenever(repository.getLogsByDate(any())).thenReturn(flowOf(emptyList()))
        
        viewModel = LogEntryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `upsertLogEntry calls repository insertLog`() = runTest {
        val logEntry = LogEntry(date = "2026-04-26", session = "Sáng", bgBefore = 5.5)
        viewModel.upsertLogEntry(logEntry)
        verify(repository).insertLog(logEntry)
    }

    @Test
    fun `allDates are collected from repository`() = runTest {
        val entries = listOf(
            LogEntry(date = "2026-04-26", session = "Sáng"),
            LogEntry(date = "2026-04-25", session = "Trưa")
        )
        whenever(repository.getAllLogs()).thenReturn(flowOf(entries))

        // Create a new ViewModel to collect from the mocked flow in its init block
        val viewModel = LogEntryViewModel(repository)
        
        // Use backgroundScope to collect the flow so it becomes active
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.allDates.collect {}
        }

        assertEquals(listOf("2026-04-26", "2026-04-25"), viewModel.allDates.value)
        job.cancel()
    }

    @Test
    fun `entriesForSelectedDate are collected from repository`() = runTest {
        val date = "2026-04-26"
        val entries = listOf(LogEntry(date = date, session = "Sáng", bgBefore = 5.5))
        whenever(repository.getLogsByDate(date)).thenReturn(flowOf(entries))

        viewModel.selectDate(date)
        
        // Use backgroundScope to collect the flow
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entriesForSelectedDate.collect {}
        }

        assertEquals(entries, viewModel.entriesForSelectedDate.value)
        job.cancel()
    }
}
