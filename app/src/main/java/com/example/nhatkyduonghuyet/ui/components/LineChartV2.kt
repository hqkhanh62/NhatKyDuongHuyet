package com.example.nhatkyduonghuyet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.nhatkyduonghuyet.viewmodel.MultiSeriesPoint
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun LineChartV2(
    data: List<MultiSeriesPoint>,
    showBefore: Boolean = true,
    showAfter: Boolean = true,
    showDaily: Boolean = true
) {
    if (data.isEmpty()) return

    val seriesList = mutableListOf<List<FloatEntry>>()
    val lineColors = mutableListOf<Color>()

    if (showBefore) {
        val seriesBefore = data.mapIndexedNotNull { index, point ->
            point.avgBefore?.let { FloatEntry(index.toFloat(), it.toFloat()) }
        }
        if (seriesBefore.isNotEmpty()) {
            seriesList.add(seriesBefore)
            lineColors.add(Color(0xFF2196F3)) // Blue
        }
    }

    if (showAfter) {
        val seriesAfter = data.mapIndexedNotNull { index, point ->
            point.avgAfter?.let { FloatEntry(index.toFloat(), it.toFloat()) }
        }
        if (seriesAfter.isNotEmpty()) {
            seriesList.add(seriesAfter)
            lineColors.add(Color(0xFFF44336)) // Red
        }
    }

    if (showDaily) {
        val seriesDaily = data.mapIndexedNotNull { index, point ->
            point.avgDaily?.let { FloatEntry(index.toFloat(), it.toFloat()) }
        }
        if (seriesDaily.isNotEmpty()) {
            seriesList.add(seriesDaily)
            lineColors.add(Color(0xFF4CAF50)) // Green
        }
    }

    if (seriesList.isEmpty()) return

    val model = entryModelOf(*seriesList.toTypedArray())

    Chart(
        chart = lineChart(
            lines = lineColors.map { color ->
                lineSpec(lineColor = color)
            }
        ),
        model = model
    )
}
