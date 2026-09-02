package com.example.nhatkyduonghuyet.ui.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nhatkyduonghuyet.domain.GlucoseRiskLevel

@Composable
fun AiPredictionRow(morningVal: Float, morningRisk: String, afternoonVal: Float, afternoonRisk: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AiCard(label = "SÁNG (Trước ăn)", value = morningVal, risk = morningRisk, modifier = Modifier.weight(1f).fillMaxHeight())
        AiCard(label = "Chiều (Trước ngủ)", value = afternoonVal, risk = afternoonRisk, modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
fun AiCard(label: String, value: Float, risk: String, modifier: Modifier = Modifier) {
    val animatedValue by animateFloatAsState(targetValue = value, animationSpec = tween(1000))

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF0D47A1), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${"%.1f".format(animatedValue)}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = Color(0xFF0D47A1)
            )
            Text("mmol/L", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            val riskColor = when (risk) {
                GlucoseRiskLevel.LOW.label -> Color(0xFFD32F2F)
                GlucoseRiskLevel.HIGH.label -> Color(0xFFFF9800)
                GlucoseRiskLevel.VERY_HIGH.label -> Color(0xFFF44336)
                else -> Color(0xFF4CAF50)
            }

            Surface(
                color = riskColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = risk,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = riskColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
