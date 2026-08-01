package com.example.nhatkyduonghuyet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry

@Composable
fun SummaryCard(logs: List<LogEntry>) {
    val allValues = logs.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
    val avg = if (allValues.isNotEmpty()) allValues.average() else 0.0
    val max = if (allValues.isNotEmpty()) allValues.maxOrNull() else 0.0
    val min = if (allValues.isNotEmpty()) allValues.minOrNull() else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tóm tắt chỉ số", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Trung bình", style = MaterialTheme.typography.bodySmall)
                    Text("%.1f".format(avg), style = MaterialTheme.typography.headlineSmall)
                }
                Column {
                    Text("Cao nhất", style = MaterialTheme.typography.bodySmall)
                    Text("%.1f".format(max), style = MaterialTheme.typography.headlineSmall)
                }
                Column {
                    Text("Thấp nhất", style = MaterialTheme.typography.bodySmall)
                    Text("%.1f".format(min), style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}
