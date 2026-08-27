package com.example.nhatkyduonghuyet.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEntryCard(
    sessionName: String,
    logEntryState: MutableState<LogEntry>,
    scanner: GlucoseScanner,
    onSave: (LogEntry) -> Unit
) {
    var logEntry by logEntryState
    var cameraField by remember { mutableStateOf<String?>(null) }

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
                val value = speech.replace(",", ".")
                    .filter { it.isDigit() || it == '.' }
                    .toDoubleOrNull()
                if (value != null) {
                    updatedEntry = updatedEntry.copy(bgBefore = value)
                    bgBeforeText = value.toString()
                }
            }
            "bgAfter" -> {
                val value = speech.replace(",", ".")
                    .filter { it.isDigit() || it == '.' }
                    .toDoubleOrNull()
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

    val sessionColor = when (sessionName) {
        "Sáng" -> Color(0xFF1976D2)
        "Trưa" -> Color(0xFFF57C00)
        "Chiều" -> Color(0xFF7B1FA2)
        "Tối" -> Color(0xFF388E3C)
        else -> MaterialTheme.colorScheme.primary
    }

    val cardBgColor = when (sessionName) {
        "Sáng" -> Color(0xFFE3F2FD)
        "Trưa" -> Color(0xFFFFF3E0)
        "Chiều" -> Color(0xFFF3E5F5)
        "Tối" -> Color(0xFFE8F5E9)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = sessionName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = sessionColor
            )
            Spacer(modifier = Modifier.height(12.dp))

            SmartInputTextField(
                value = logEntry.medType ?: "",
                onValueChange = { logEntry = logEntry.copy(medType = it.ifEmpty { null }) },
                label = "Loại insulin/thuốc",
                onVoiceResult = { handleVoiceResult("medType", it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmartInputTextField(
                    value = logEntry.dose ?: "",
                    onValueChange = { logEntry = logEntry.copy(dose = it.ifEmpty { null }) },
                    label = "Liều",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onVoiceResult = { handleVoiceResult("dose", it) }
                )
                SmartInputTextField(
                    value = logEntry.time ?: "",
                    onValueChange = { logEntry = logEntry.copy(time = it.ifEmpty { null }) },
                    label = "Giờ",
                    modifier = Modifier.weight(1f),
                    onVoiceResult = { handleVoiceResult("time", it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SmartInputTextField(
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
                onVoiceResult = { handleVoiceResult("bgBefore", it) },
                onCameraClick = { cameraField = "bgBefore" }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SmartInputTextField(
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
                onVoiceResult = { handleVoiceResult("bgAfter", it) },
                onCameraClick = { cameraField = "bgAfter" }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SmartInputTextField(
                value = logEntry.note ?: "",
                onValueChange = { logEntry = logEntry.copy(note = it.ifEmpty { null }) },
                label = "Ghi chú",
                onVoiceResult = { handleVoiceResult("note", it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

                        Button(
                onClick = { onSave(logEntry) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = sessionColor)
            ) {
                Text("LƯU DỮ LIỆU", fontWeight = FontWeight.Bold)
            }
        }
    }

    cameraField?.let { field ->
        CameraScannerDialog(
            scanner = scanner,
            onDismiss = { cameraField = null },
            onResult = { result ->
                val updatedEntry = when (field) {
                    "bgBefore" -> {
                        bgBeforeText = result.value.toInputText()
                        logEntry.copy(bgBefore = result.value.toDouble())
                    }
                    else -> {
                        bgAfterText = result.value.toInputText()
                        logEntry.copy(bgAfter = result.value.toDouble())
                    }
                }
                logEntry = updatedEntry
                onSave(updatedEntry)
            }
        )
    }
}

private fun Float.toInputText(): String =
    String.format(Locale.US, "%.1f", this)
