package com.example.nhatkyduonghuyet.ui.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState

@Composable
fun AnimatedStatRow(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedStatCard("Max", state.max, Color.Red, modifier = Modifier.weight(1f))
        AnimatedStatCard("Avg", state.avg, Color.Blue, modifier = Modifier.weight(1f))
        AnimatedStatCard("High %", state.highRate.toDouble(), Color.Magenta, modifier = Modifier.weight(1f))
    }
}

@Composable
fun AnimatedStatCard(title: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        label = "StatAnim"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = String.format("%.1f", animatedValue),
                color = color,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun StatRowPreview() {
    MaterialTheme {
        AnimatedStatRow(state = DashboardUiState(max = 17.8, avg = 9.6, highRate = 45))
    }
}
