package com.example.nhatkyduonghuyet.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState

@Composable
fun RiskSummaryCard(state: DashboardUiState) {
    val riskLevel = when {
        state.max > 15 -> "Nguy hiểm cao"
        state.max > 13 -> "Cảnh báo"
        else -> "Ổn định"
    }

    val color = when (riskLevel) {
        "Nguy hiểm cao" -> Color.Red
        "Cảnh báo" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Tình trạng: $riskLevel",
            modifier = Modifier.padding(16.dp),
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
fun InsightList(insights: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        insights.forEach { insight ->
            AssistChip(
                onClick = {},
                label = { Text(insight) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun RiskSummaryPreview() {
    MaterialTheme {
        RiskSummaryCard(state = DashboardUiState(max = 17.8))
    }
}
