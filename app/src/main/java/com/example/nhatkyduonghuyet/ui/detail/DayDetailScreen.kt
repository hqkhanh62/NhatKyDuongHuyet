package com.example.nhatkyduonghuyet.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
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

    // State cho từng buổi trong ngày
    val sessionStates: Map<String, MutableState<LogEntry>> = remember(selectedDate, entries) {
        sessions.associateWith { sessionName ->
            val existingEntry = entries.find { it.session == sessionName }
            mutableStateOf(
                existingEntry ?: LogEntry(
                    id = 0L,
                    date = selectedDate,
                    session = sessionName,
                    medType = null,
                    dose = null,
                    time = null,
                    bgBefore = null,
                    bgAfter = null,
                    note = null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEntryCard(
    sessionName: String,
    logEntryState: MutableState<LogEntry>,
    onSave: (LogEntry) -> Unit
) {
    var logEntry by logEntryState

    // State text riêng cho các ô nhập số thập phân
    var bgBeforeText by remember(logEntry.id, logEntry.session) {
        mutableStateOf(logEntry.bgBefore?.toString() ?: "")
    }
    var bgAfterText by remember(logEntry.id, logEntry.session) {
        mutableStateOf(logEntry.bgAfter?.toString() ?: "")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = sessionName,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.medType ?: "",
                onValueChange = {
                    logEntry = logEntry.copy(
                        medType = it.ifEmpty { null }
                    )
                },
                label = { Text("Loại insulin/thuốc") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.dose ?: "",
                onValueChange = {
                    logEntry = logEntry.copy(
                        dose = it.ifEmpty { null }
                    )
                },
                label = { Text("Liều (đv/viên)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.time ?: "",
                onValueChange = {
                    logEntry = logEntry.copy(
                        time = it.ifEmpty { null }
                    )
                },
                label = { Text("Giờ tiêm/uống") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Đường huyết trước (mmol/L) – cho phép số thập phân
            OutlinedTextField(
                value = bgBeforeText,
                onValueChange = { newValue ->
                    // Cho phép xóa hết
                    if (newValue.isEmpty()) {
                        bgBeforeText = ""
                        logEntry = logEntry.copy(bgBefore = null)
                        return@OutlinedTextField
                    }

                    // Chấp nhận cả . và , làm dấu thập phân
                    val normalized = newValue.replace(',', '.')

                    // Chỉ cho phép dạng: số, số., số.x
                    val decimalRegex = Regex("""\d+(\.\d*)?""")
                    if (normalized.matches(decimalRegex)) {
                        bgBeforeText = newValue
                        logEntry = logEntry.copy(
                            bgBefore = normalized.toDoubleOrNull()
                        )
                    }
                    // Nếu không match (gõ chữ, nhiều dấu .), bỏ qua thay đổi
                },
                label = { Text("Đường huyết trước (mmol/L)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Đường huyết sau 2 giờ (mmol/L) – cho phép số thập phân
            OutlinedTextField(
                value = bgAfterText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty()) {
                        bgAfterText = ""
                        logEntry = logEntry.copy(bgAfter = null)
                        return@OutlinedTextField
                    }

                    val normalized = newValue.replace(',', '.')

                    val decimalRegex = Regex("""\d+(\.\d*)?""")
                    if (normalized.matches(decimalRegex)) {
                        bgAfterText = newValue
                        logEntry = logEntry.copy(
                            bgAfter = normalized.toDoubleOrNull()
                        )
                    }
                },
                label = { Text("Đường huyết sau 2 giờ (mmol/L)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.note ?: "",
                onValueChange = {
                    logEntry = logEntry.copy(
                        note = it.ifEmpty { null }
                    )
                },
                label = { Text("Triệu chứng/Ghi chú") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.bpSys?.toString() ?: "",
                onValueChange = {
                    logEntry = logEntry.copy(
                        bpSys = it.toIntOrNull()
                    )
                },
                label = { Text("Huyết áp tâm thu (mmHg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = logEntry.bpDia?.toString() ?: "",
                onValueChange = {
                    logEntry = logEntry.copy(
                        bpDia = it.toIntOrNull()
                    )
                },
                label = { Text("Huyết áp tâm trương (mmHg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onSave(logEntry) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu")
            }
        }
    }
}