package com.example.nhatkyduonghuyet.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.LogEntry
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    navController: NavController,
    viewModel: LogEntryViewModel,
    selectedDate: String
) {
    val entries by viewModel.entriesForSelectedDate.collectAsState()
    val sessions = listOf("Sáng", "Trưa", "Chiều", "Tối")

    // State for each session's fields
    val sessionStates = remember(selectedDate, entries) {
        sessions.associateWith { sessionName ->
            val existingEntry = entries.find { it.session == sessionName }
            mutableStateOf(existingEntry ?: LogEntry(date = selectedDate, session = sessionName))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết ngày: $selectedDate") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            items(sessions) {
                SessionEntryCard(
                    sessionName = it,
                    logEntryState = sessionStates[it]!!,
                    onSave = { logEntry ->
                        viewModel.upsertLogEntry(logEntry)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEntryCard(
    sessionName: String,
    logEntryState: MutableState<LogEntry>,
    onSave: (LogEntry) -> Unit
) {
    var logEntry by logEntryState

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = sessionName, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.medType ?: "",
                onValueChange = { logEntry = logEntry.copy(medType = it.ifEmpty { null }) },
                label = { Text("Loại insulin/thuốc") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.dose ?: "",
                onValueChange = { logEntry = logEntry.copy(dose = it.ifEmpty { null }) },
                label = { Text("Liều (đv/viên)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.time ?: "",
                onValueChange = { logEntry = logEntry.copy(time = it.ifEmpty { null }) },
                label = { Text("Giờ tiêm/uống") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.bgBefore?.toString() ?: "",
                onValueChange = { logEntry = logEntry.copy(bgBefore = it.toDoubleOrNull()) },
                label = { Text("Đường huyết trước (mmol/L)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.bgAfter?.toString() ?: "",
                onValueChange = { logEntry = logEntry.copy(bgAfter = it.toDoubleOrNull()) },
                label = { Text("Đường huyết sau 2 giờ (mmol/L)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.note ?: "",
                onValueChange = { logEntry = logEntry.copy(note = it.ifEmpty { null }) },
                label = { Text("Triệu chứng/Ghi chú") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { onSave(logEntry) }, modifier = Modifier.fillMaxWidth()) {
                Text("Lưu")
            }
        }
    }
}
