package com.example.nhatkyduonghuyet.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.ui.chart.aggregateBySession
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

@Composable
fun GlucoseChartPro(entries: List<LogEntry>) {
    var selectedX by remember { mutableStateOf<Float?>(null) }
    val dataSlice = remember(entries) { aggregateBySession(entries).takeLast(10) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Xu hướng đường huyết (Marker)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
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
                            axisLeft.setDrawGridLines(true)
                            legend.isEnabled = true

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
                        val chartEntries = dataSlice.mapIndexed { i, point ->
                            Entry(i.toFloat(), (point.avgDaily ?: 0.0).toFloat()) 
                        }
                        
                        chart.xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val idx = value.toInt()
                                return if (selectedX != null && Math.abs(value - selectedX!!) < 0.1f && idx >= 0 && idx < dataSlice.size) {
                                    dataSlice[idx].dateLabel
                                } else ""
                            }
                        }

                        if (chartEntries.isNotEmpty()) {
                            val dataSet = LineDataSet(chartEntries, "mmol/L").apply {
                                val primaryColor = android.graphics.Color.BLUE
                                color = primaryColor
                                setCircleColor(primaryColor)
                                lineWidth = 3f
                                circleRadius = 5f
                                setDrawFilled(true)
                                fillAlpha = 40
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                setDrawValues(false)
                            }
                            chart.data = LineData(dataSet)
                            chart.notifyDataSetChanged()
                            chart.invalidate()
                        }
                    }
                )
            }
        }
    }
}
