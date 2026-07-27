package com.example.nhatkyduonghuyet.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry

@Composable
fun SessionCardV2(
    sessionName: String,
    logEntry: LogEntry,
    onValueChange: (LogEntry) -> Unit
) {
    var bgBefore by remember { mutableStateOf(logEntry.bgBefore?.toString() ?: "") }

    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {

            Text(sessionName, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = bgBefore,
                onValueChange = {
                    bgBefore = it
                    onValueChange(logEntry.copy(bgBefore = it.toDoubleOrNull()))
                },
                label = { Text("Đường huyết trước") }
            )
        }
    }
}