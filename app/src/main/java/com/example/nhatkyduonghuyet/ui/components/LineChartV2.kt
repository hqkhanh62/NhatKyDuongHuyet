package com.example.nhatkyduonghuyet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.nhatkyduonghuyet.ui.chart.SessionPoint
import com.example.nhatkyduonghuyet.viewmodel.MultiSeriesPoint
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun LineChartV2(
    points: List<SessionPoint>,
    showMorning: Boolean = true,
    showNoon: Boolean = true,
    showEvening: Boolean = true,
    showNight: Boolean = true,
    showDailyAvg: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val seriesList = mutableListOf<List<FloatEntry>>()
    val lineColors = mutableListOf<Color>()

    fun addSeries(values: List<Double?>, color: Color) {
        val series = values.mapIndexedNotNull { index, value ->
            value?.let { FloatEntry(index.toFloat(), it.toFloat()) }
        }
        if (series.isNotEmpty()) {
            seriesList.add(series)
            lineColors.add(color)
        }
    }

    if (showMorning) addSeries(points.map { it.avgMorning }, Color(0xFF2196F3))
    if (showNoon) addSeries(points.map { it.avgNoon }, Color(0xFFFF9800))
    if (showEvening) addSeries(points.map { it.avgEvening }, Color(0xFFF44336))
    if (showNight) addSeries(points.map { it.avgNight }, Color(0xFF9C27B0))
    if (showDailyAvg) addSeries(points.map { it.avgDaily }, Color(0xFF4CAF50))

    if (seriesList.isEmpty()) return

    CommonLineChart(
        modifier = modifier,
        seriesList = seriesList,
        lineColors = lineColors,
        dateLabels = points.map { it.dateLabel }
    )
}

@Composable
fun LineChartV2(
    data: List<MultiSeriesPoint>,
    showBefore: Boolean = true,
    showAfter: Boolean = true,
    showDaily: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val seriesList = mutableListOf<List<FloatEntry>>()
    val lineColors = mutableListOf<Color>()

    fun addSeries(values: List<Double?>, color: Color) {
        val series = values.mapIndexedNotNull { index, value ->
            value?.let { FloatEntry(index.toFloat(), it.toFloat()) }
        }
        if (series.isNotEmpty()) {
            seriesList.add(series)
            lineColors.add(color)
        }
    }

    if (showBefore) addSeries(data.map { it.avgBefore }, Color(0xFF2196F3))
    if (showAfter) addSeries(data.map { it.avgAfter }, Color(0xFFF44336))
    if (showDaily) addSeries(data.map { it.avgDaily }, Color(0xFF4CAF50))

    if (seriesList.isEmpty()) return

    CommonLineChart(
        modifier = modifier,
        seriesList = seriesList,
        lineColors = lineColors,
        dateLabels = data.map { it.date.substringAfterLast("-") } // Simple label dd
    )
}

@Composable
private fun CommonLineChart(
    modifier: Modifier,
    seriesList: List<List<FloatEntry>>,
    lineColors: List<Color>,
    dateLabels: List<String>
) {
    val model = entryModelOf(*seriesList.toTypedArray())
    
    val bottomAxisValueFormatter = remember(dateLabels) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            dateLabels.getOrNull(value.toInt()) ?: ""
        }
    }

    Chart(
        modifier = modifier,
        chart = lineChart(
            lines = lineColors.map { color ->
                lineSpec(lineColor = color)
            },
            axisValuesOverrider = AxisValuesOverrider.fixed(
                minY = 3f,
                maxY = 15f
            )
        ),
        model = model,
        startAxis = rememberStartAxis(
            valueFormatter = { value, _ -> "%.1f".format(value) }
        ),
        bottomAxis = rememberBottomAxis(
            valueFormatter = bottomAxisValueFormatter,
            labelRotationDegrees = 45f
        ),
        chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = true),
        isZoomEnabled = true
    )
}
