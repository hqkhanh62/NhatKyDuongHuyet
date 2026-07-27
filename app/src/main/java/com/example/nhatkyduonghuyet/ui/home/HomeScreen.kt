package com.example.nhatkyduonghuyet.ui.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    // Logic xử lý giọng nói chung
    fun handleVoiceInput(field: String, speech: String) {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        var updatedEntry = logEntry.copy()
        
        when (field) {
            "medType" -> updatedEntry = updatedEntry.copy(medType = speech, time = currentTime)
            "dose" -> updatedEntry = updatedEntry.copy(dose = speech, time = currentTime)
            "time" -> updatedEntry = updatedEntry.copy(time = speech)
            "bgBefore" -> {
                val value = speech.replace(",", ".").filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                if (value != null) {
                    updatedEntry = updatedEntry.copy(bgBefore = value)
                    bgBeforeStr = value.toString()
                }
            }
            "bgAfter" -> {
                val value = speech.replace(",", ".").filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                if (value != null) {
                    updatedEntry = updatedEntry.copy(bgAfter = value)
                    bgAfterStr = value.toString()
                }
            }
            "bpSys" -> {
                speech.filter { it.isDigit() }.toIntOrNull()?.let { updatedEntry = updatedEntry.copy(bpSys = it) }
            }
            "bpDia" -> {
                speech.filter { it.isDigit() }.toIntOrNull()?.let { updatedEntry = updatedEntry.copy(bpDia = it) }
            }
            "heartRate" -> {
                speech.filter { it.isDigit() }.toIntOrNull()?.let { updatedEntry = updatedEntry.copy(heartRate = it) }
            }
            "note" -> updatedEntry = updatedEntry.copy(note = speech)
        }
        
        logEntry = updatedEntry
        onSave(updatedEntry)
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

            VoiceEnabledTextField(
                value = logEntry.medType ?: "",
                onValueChange = { logEntry = logEntry.copy(medType = if (it.isEmpty()) null else it) },
                label = "Loại insulin/thuốc",
                onVoiceResult = { handleVoiceInput("medType", it) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VoiceEnabledTextField(
                    value = logEntry.dose ?: "",
                    onValueChange = { logEntry = logEntry.copy(dose = if (it.isEmpty()) null else it) },
                    label = "Liều dùng",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onVoiceResult = { handleVoiceInput("dose", it) }
                )
                VoiceEnabledTextField(
                    value = logEntry.time ?: "",
                    onValueChange = { logEntry = logEntry.copy(time = if (it.isEmpty()) null else it) },
                    label = "Giờ giấc",
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("07:30") },
                    onVoiceResult = { handleVoiceInput("time", it) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VoiceEnabledTextField(
                    value = bgBeforeStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null || input.endsWith(".")) {
                            bgBeforeStr = input
                            logEntry = logEntry.copy(bgBefore = input.toDoubleOrNull())
                        }
                    },
                    label = "ĐH Trước",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    onVoiceResult = { handleVoiceInput("bgBefore", it) }
                )
                VoiceEnabledTextField(
                    value = bgAfterStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null || input.endsWith(".")) {
                            bgAfterStr = input
                            logEntry = logEntry.copy(bgAfter = input.toDoubleOrNull())
                        }
                    },
                    label = "ĐH Sau 2h",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    onVoiceResult = { handleVoiceInput("bgAfter", it) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // --- HA tâm thu ---
            VoiceEnabledTextField(
                value = logEntry.bpSys?.toString() ?: "",
                onValueChange = {
                    val v = it.toIntOrNull()
                    logEntry = logEntry.copy(bpSys = v)
                },
                label = "HA tâm thu (mmHg)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onVoiceResult = { handleVoiceInput("bpSys", it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- HA tâm trương ---
            VoiceEnabledTextField(
                value = logEntry.bpDia?.toString() ?: "",
                onValueChange = {
                    val v = it.toIntOrNull()
                    logEntry = logEntry.copy(bpDia = v)
                },
                label = "HA tâm trương (mmHg)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onVoiceResult = { handleVoiceInput("bpDia", it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- Nhịp tim ---
            VoiceEnabledTextField(
                value = logEntry.heartRate?.toString() ?: "",
                onValueChange = {
                    val v = it.toIntOrNull()
                    logEntry = logEntry.copy(heartRate = v)
                },
                label = "Nhịp tim (lần/phút)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onVoiceResult = { handleVoiceInput("heartRate", it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- Triệu chứng / Ghi chú ---
            VoiceEnabledTextField(
                value = logEntry.note ?: "",
                onValueChange = { logEntry = logEntry.copy(note = if (it.isEmpty()) null else it) },
                label = "Triệu chứng / Ghi chú thêm",
                minLines = 2,
                onVoiceResult = { handleVoiceInput("note", it) }
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

@Composable
fun VoiceEnabledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onVoiceResult: (String) -> Unit,
    placeholder: @Composable (() -> Unit)? = null,
    minLines: Int = 1
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                data?.get(0)?.let { onVoiceResult(it) }
            }
        }
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        placeholder = placeholder,
        minLines = minLines,
        trailingIcon = {
            IconButton(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Đang nghe: $label...")
                }
                launcher.launch(intent)
            }) {
                Icon(
                    imageVector = Icons.Default.Mic, 
                    contentDescription = "Giọng nói",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        shape = MaterialTheme.shapes.medium
    )
}
