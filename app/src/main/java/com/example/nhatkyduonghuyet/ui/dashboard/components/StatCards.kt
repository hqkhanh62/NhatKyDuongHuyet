package com.example.nhatkyduonghuyet.ui.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nhatkyduonghuyet.ui.dashboard.ComparisonData
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState

@Composable
fun AnimatedStatRow(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedStatCard(
            title = "Max", 
            value = state.max, 
            color = Color.Red, 
            compare = state.maxCompare,
            modifier = Modifier.weight(1f)
        )
        AnimatedStatCard(
            title = "Avg", 
            value = state.avg, 
            color = Color.Blue, 
            compare = state.avgCompare,
            modifier = Modifier.weight(1f)
        )
        AnimatedStatCard(
            title = "High %", 
            value = state.highRate.toDouble(), 
            color = Color.Magenta, 
            compare = state.highRateCompare,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AnimatedStatCard(
    title: String, 
    value: Double, 
    color: Color, 
    compare: ComparisonData?,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        label = "StatAnim"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            Text(
                text = String.format("%.1f", animatedValue),
                color = color,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            compare?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val isUp = it.diff > 0
                    Icon(
                        imageVector = if (isUp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (it.isBetter) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${String.format("%.0f", Math.abs(it.percentChange))}%",
                        color = if (it.isBetter) Color(0xFF4CAF50) else Color(0xFFF44336),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } ?: Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
