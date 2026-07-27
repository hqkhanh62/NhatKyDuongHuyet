package com.example.nhatkyduonghuyet.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check // Sử dụng icon Check có sẵn trong bộ core
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// TODO: Kiểm tra và sửa lại đường dẫn package chính xác của LogEntry nếu file nằm ở thư mục khác:
// G:\NhatKyDuongHuyet_PRO_MAX_FINAL\app\src\main\java\com\example\nhatkyduonghuyet\data\local\entity\LogEntry.kt
// Ví dụ: import com.example.nhatkyduonghuyet.data.model.LogEntry
// Hoặc:  import com.example.nhatkyduonghuyet.data.entity.LogEntry
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

    val sessionStates = remember(selectedDate, entries) {
        sessions.associateWith { sessionName ->
            val existingEntry = entries.firstOrNull { entry ->
                entry.session == sessionName
            }
            mutableStateOf(
                existingEntry ?: LogEntry(
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
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Chi tiết: $selectedDate",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sessions) { sessionName ->
                    SessionEntryCard(
                        sessionName = sessionName,
                        logEntryState = sessionStates[sessionName]!!,
                        onSave = { logEntry ->
                            val entryToSave = logEntry.copy(date = selectedDate)
                            viewModel.upsertLogEntry(entryToSave)
                        }
                    )
                }
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

    var bgBeforeStr by remember(logEntry.bgBefore) { 
        mutableStateOf(logEntry.bgBefore?.toString() ?: "") 
    }
    var bgAfterStr by remember(logEntry.bgAfter) { 
        mutableStateOf(logEntry.bgAfter?.toString() ?: "") 
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Buổi $sessionName", 
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            OutlinedTextField(
                value = logEntry.medType ?: "",
                onValueChange = { logEntry = logEntry.copy(medType = it.ifEmpty { null }) },
                label = { Text("Loại insulin/thuốc") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = logEntry.dose ?: "",
                    onValueChange = { logEntry = logEntry.copy(dose = it.ifEmpty { null }) },
                    label = { Text("Liều dùng") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = logEntry.time ?: "",
                    onValueChange = { logEntry = logEntry.copy(time = it.ifEmpty { null }) },
                    label = { Text("Giờ giấc") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("07:30") },
                    shape = MaterialTheme.shapes.medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = bgBeforeStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null || input.endsWith(".")) {
                            bgBeforeStr = input
                            logEntry = logEntry.copy(bgBefore = input.toDoubleOrNull())
                        }
                    },
                    label = { Text("ĐH Trước") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = bgAfterStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null || input.endsWith(".")) {
                            bgAfterStr = input
                            logEntry = logEntry.copy(bgAfter = input.toDoubleOrNull())
                        }
                    },
                    label = { Text("ĐH Sau 2h") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // --- HA tâm thu ---
            OutlinedTextField(
                value = logEntry.bpSys?.toString() ?: "",
                onValueChange = {
                    val v = it.toIntOrNull()
                    logEntry = logEntry.copy(bpSys = v)
                },
                label = { Text("HA tâm thu (mmHg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- HA tâm trương ---
            OutlinedTextField(
                value = logEntry.bpDia?.toString() ?: "",
                onValueChange = {
                    val v = it.toIntOrNull()
                    logEntry = logEntry.copy(bpDia = v)
                },
                label = { Text("HA tâm trương (mmHg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- Nhịp tim ---
            OutlinedTextField(
                value = logEntry.heartRate?.toString() ?: "",
                onValueChange = {
                    val v = it.toIntOrNull()
                    logEntry = logEntry.copy(heartRate = v)
                },
                label = { Text("Nhịp tim (lần/phút)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- Triệu chứng / Ghi chú ---
            OutlinedTextField(
                value = logEntry.note ?: "",
                onValueChange = { logEntry = logEntry.copy(note = it.ifEmpty { null }) },
                label = { Text("Triệu chứng / Ghi chú thêm") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onSave(logEntry) }, 
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu dữ liệu buổi $sessionName", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}