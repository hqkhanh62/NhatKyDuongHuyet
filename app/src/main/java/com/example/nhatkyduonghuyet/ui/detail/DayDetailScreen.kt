package com.example.nhatkyduonghuyet.ui.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    navController: NavController,
    viewModel: LogEntryViewModel,
    selectedDate: String
) {
    LaunchedEffect(selectedDate) {
        viewModel.selectDate(selectedDate)
    }

    val entries by viewModel.entriesForSelectedDate.collectAsState()
    val sessions = listOf("Sáng", "Trưa", "Chiều", "Tối")

    val sessionStates = remember(selectedDate, entries) {
        sessions.associateWith { sessionName ->
            val existingEntry = entries.find { it.session == sessionName }
            mutableStateOf(
                existingEntry ?: LogEntry(
                    id = 0L,
                    date = selectedDate,
                    session = sessionName
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết ngày: $selectedDate") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            items(sessions) { sessionName ->
                val logEntryState = sessionStates[sessionName]!!
                SessionEntryCard(
                    sessionName = sessionName,
                    logEntryState = logEntryState,
                    onSave = { logEntry ->
                        viewModel.upsertLogEntry(logEntry)
                    }
                )
            }
        }
    }
}