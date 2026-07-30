package com.example.nhatkyduonghuyet.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

@Composable
fun GlucoseChartPro(state: DashboardUiState) {
    var selectedX by remember { mutableStateOf<Float?>(null) }
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Health Comparison Trend", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("Hiện tại", Color.Blue)
                LegendItem("Kì trước", Color.Gray, isDashed = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(true)
                            setPinchZoom(true)
                            
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                granularity = 1f
                                setDrawLabels(true) 
                            }
                            axisRight.isEnabled = false
                            axisLeft.apply {
                                setDrawGridLines(true)
                                axisMinimum = 3f
                                axisMaximum = 20f
                            }
                            legend.isEnabled = false

                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    selectedX = e?.x
                                }
                                override fun onNothingSelected() {
                                    selectedX = null
                                }
                            })
                        }
                    },
                    update = { chart ->
                        val currentEntries = state.currentPeriodPoints.map { Entry(it.xIndex.toFloat(), it.value.toFloat()) }
                        val prevEntries = state.previousPeriodPoints.map { Entry(it.xIndex.toFloat(), it.value.toFloat()) }
                        
                        chart.xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val idx = value.toInt()
                                return if (selectedX != null && Math.abs(value - selectedX!!) < 0.1f && idx >= 0 && idx < state.currentPeriodPoints.size) {
                                    state.currentPeriodPoints[idx].dateLabel
                                } else ""
                            }
                        }

                        val dataSets = mutableListOf<ILineDataSet>()

                        if (prevEntries.isNotEmpty()) {
                            dataSets.add(LineDataSet(prevEntries, "Kì trước").apply {
                                color = android.graphics.Color.LTGRAY
                                setDrawCircles(false)
                                lineWidth = 1.5f
                                enableDashedLine(10f, 10f, 0f)
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                setDrawValues(false)
                                isHighlightEnabled = false
                            })
                        }

                        if (currentEntries.isNotEmpty()) {
                            dataSets.add(LineDataSet(currentEntries, "Hiện tại").apply {
                                color = android.graphics.Color.BLUE
                                setCircleColor(android.graphics.Color.BLUE)
                                lineWidth = 3f
                                circleRadius = 4f
                                setDrawFilled(true)
                                fillAlpha = 30
                                fillColor = android.graphics.Color.BLUE
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                setDrawValues(false)
                            })
                        }

                        chart.data = LineData(dataSets)
                        chart.notifyDataSetChanged()
                        chart.invalidate()
                    }
                )
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, isDashed: Boolean = false) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 24.dp, height = 4.dp)) {
            if (isDashed) {
                drawLine(color = color, start = Offset.Zero, end = Offset(size.width, 0f), strokeWidth = 4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            } else {
                drawLine(color = color, start = Offset.Zero, end = Offset(size.width, 0f), strokeWidth = 8f)
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}
