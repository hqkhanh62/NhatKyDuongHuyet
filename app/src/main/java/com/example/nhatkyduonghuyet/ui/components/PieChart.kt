package com.example.nhatkyduonghuyet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2

data class PieChartData(
    val label: String,
    val count: Int,
    val percent: Int,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val totalCount = data.sumOf { it.count }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val angle = Math.toDegrees(
                            atan2(
                                (offset.y - centerY).toDouble(),
                                (offset.x - centerX).toDouble()
                            )
                        ).let { if (it < 0) it + 360 else it }

                        var currentAngle = 0f
                        data.forEachIndexed { index, pieData ->
                            val sweepAngle = (pieData.count.toFloat() / totalCount) * 360f
                            if (angle >= currentAngle && angle <= currentAngle + sweepAngle) {
                                selectedIndex = if (selectedIndex == index) -1 else index
                                return@detectTapGestures
                            }
                            currentAngle += sweepAngle
                        }
                        selectedIndex = -1
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize = size.minDimension
                val radius = canvasSize / 2
                var startAngle = 0f

                data.forEachIndexed { index, pieData ->
                    val sweepAngle = if (totalCount > 0) (pieData.count.toFloat() / totalCount) * 360f else 0f
                    if (sweepAngle > 0) {
                        val isSelected = selectedIndex == index
                        val strokeWidth = if (isSelected) 60f else 40f
                        
                        drawArc(
                            color = pieData.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            size = Size(canvasSize - 80f, canvasSize - 80f),
                            topLeft = Offset(40f, 40f)
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            if (selectedIndex != -1 && totalCount > 0) {
                val selectedData = data[selectedIndex]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedData.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = selectedData.color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedData.count} bản ghi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedData.percent}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = if (totalCount > 0) "Chạm để xem" else "Không có dữ liệu",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            data.forEach { pieData ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(pieData.color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = pieData.label, fontSize = 12.sp)
                }
            }
        }
    }
}
