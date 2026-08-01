package com.example.nhatkyduonghuyet.ui.components

import androidx.compose.runtime.Composable
import com.example.nhatkyduonghuyet.data.model.DailyAvgRow
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun LineChart(data: List<DailyAvgRow>) {

    val entries = data.mapIndexed { index, item ->
        FloatEntry(index.toFloat(), item.averageValue.toFloat())
    }

    val model = entryModelOf(entries)

    Chart(
        chart = lineChart(),
        model = model
    )
}
