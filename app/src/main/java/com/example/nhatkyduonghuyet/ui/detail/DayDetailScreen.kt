package com.example.nhatkyduonghuyet.ui.detail

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
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

    var bgBeforeText by remember(logEntry.id, logEntry.session) {
        mutableStateOf(logEntry.bgBefore?.toString() ?: "")
    }
    var bgAfterText by remember(logEntry.id, logEntry.session) {
        mutableStateOf(logEntry.bgAfter?.toString() ?: "")
    }

    fun handleVoiceResult(field: String, speech: String) {
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
                    bgBeforeText = value.toString()
                }
            }
            "bgAfter" -> {
                val value = speech.replace(",", ".").filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                if (value != null) {
                    updatedEntry = updatedEntry.copy(bgAfter = value)
                    bgAfterText = value.toString()
                }
            }
            "note" -> updatedEntry = updatedEntry.copy(note = speech)
        }
        
        logEntry = updatedEntry
        onSave(updatedEntry)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (sessionName) {
                "Sáng" -> Color(0xFFE3F2FD) 
                "Trưa" -> Color(0xFFFFF3E0) 
                "Chiều" -> Color(0xFFF3E5F5) 
                "Tối" -> Color(0xFFE8F5E9) 
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = sessionName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when (sessionName) {
                    "Sáng" -> Color(0xFF1976D2)
                    "Trưa" -> Color(0xFFF57C00)
                    "Chiều" -> Color(0xFF7B1FA2)
                    "Tối" -> Color(0xFF388E3C)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            VoiceEnabledTextField(
                value = logEntry.medType ?: "",
                onValueChange = { logEntry = logEntry.copy(medType = it.ifEmpty { null }) },
                label = "Loại insulin/thuốc",
                onVoiceResult = { handleVoiceResult("medType", it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceEnabledTextField(
                    value = logEntry.dose ?: "",
                    onValueChange = { logEntry = logEntry.copy(dose = it.ifEmpty { null }) },
                    label = "Liều",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onVoiceResult = { handleVoiceResult("dose", it) }
                )
                VoiceEnabledTextField(
                    value = logEntry.time ?: "",
                    onValueChange = { logEntry = logEntry.copy(time = it.ifEmpty { null }) },
                    label = "Giờ",
                    modifier = Modifier.weight(1f),
                    onVoiceResult = { handleVoiceResult("time", it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            VoiceEnabledTextField(
                value = bgBeforeText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty()) {
                        bgBeforeText = ""
                        logEntry = logEntry.copy(bgBefore = null)
                    } else {
                        val normalized = newValue.replace(',', '.')
                        if (normalized.matches(Regex("""\d+(\.\d*)?"""))) {
                            bgBeforeText = newValue
                            logEntry = logEntry.copy(bgBefore = normalized.toDoubleOrNull())
                        }
                    }
                },
                label = "Đường huyết trước (mmol/L)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                onVoiceResult = { handleVoiceResult("bgBefore", it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            VoiceEnabledTextField(
                value = bgAfterText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty()) {
                        bgAfterText = ""
                        logEntry = logEntry.copy(bgAfter = null)
                    } else {
                        val normalized = newValue.replace(',', '.')
                        if (normalized.matches(Regex("""\d+(\.\d*)?"""))) {
                            bgAfterText = newValue
                            logEntry = logEntry.copy(bgAfter = normalized.toDoubleOrNull())
                        }
                    }
                },
                label = "Đường huyết sau (mmol/L)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                onVoiceResult = { handleVoiceResult("bgAfter", it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            VoiceEnabledTextField(
                value = logEntry.note ?: "",
                onValueChange = { logEntry = logEntry.copy(note = it.ifEmpty { null }) },
                label = "Ghi chú",
                onVoiceResult = { handleVoiceResult("note", it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSave(logEntry) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (sessionName) {
                        "Sáng" -> Color(0xFF1976D2)
                        "Trưa" -> Color(0xFFF57C00)
                        "Chiều" -> Color(0xFF7B1FA2)
                        "Tối" -> Color(0xFF388E3C)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text("LƯU DỮ LIỆU", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, name = "DayDetail Review")
@Composable
fun DayDetailReviewPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(8.dp)) {
            SessionEntryCard(
                sessionName = "Sáng",
                logEntryState = remember { mutableStateOf(LogEntry(date = "2026", session = "Sáng")) },
                onSave = {}
            )
            SessionEntryCard(
                sessionName = "Trưa",
                logEntryState = remember { mutableStateOf(LogEntry(date = "2026", session = "Trưa")) },
                onSave = {}
            )
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
    onVoiceResult: (String) -> Unit
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
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
        ),
        trailingIcon = {
            IconButton(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Đang nghe: $label...")
                }
                launcher.launch(intent)
            }) {
                Icon(Icons.Default.Mic, contentDescription = "Giọng nói", tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
