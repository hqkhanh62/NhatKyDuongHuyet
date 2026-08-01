package com.example.nhatkyduonghuyet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GlucoseChart(logs: List<LogEntry>) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = logs.mapIndexed { i, log ->
                Entry(i.toFloat(), log.bgBefore?.toFloat() ?: 0f)
            }.filter { it.y > 0f }

            val dataSet = LineDataSet(entries, "Đường huyết").apply {
                lineWidth = 2f
                setDrawCircles(true)
                setDrawValues(false)
                color = android.graphics.Color.BLUE
                setCircleColor(android.graphics.Color.BLUE)
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
