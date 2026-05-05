package com.example.nhatkyduonghuyet.ui.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    navController: NavController,
    viewModel: LogEntryViewModel,
    chartType: String
) {
    val date by viewModel.currentDate.collectAsState()
    val dailyData by viewModel.getDailyChartData(date).collectAsState()
    val weeklyData by viewModel.getWeeklyChartData().collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (chartType == "daily") "Biểu đồ Hàng ngày" else "Biểu đồ Hàng tuần") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (chartType == "daily") {
                Text("Dữ liệu ngày: $date", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (dailyData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                        Text("Không có dữ liệu cho ngày này")
                    }
                } else {
                    BloodGlucoseLineChart(
                        dataPoints = dailyData,
                        label = "Đường huyết (mmol/L)",
                        modifier = Modifier.fillMaxWidth().height(250.dp)
                    )
                }
            } else {
                Text("Trung bình hàng tuần", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (weeklyData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                        Text("Chưa có dữ liệu hàng tuần")
                    }
                } else {
                    BloodGlucoseLineChart(
                        dataPoints = weeklyData,
                        label = "Trung bình (mmol/L)",
                        modifier = Modifier.fillMaxWidth().height(250.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BloodGlucoseLineChart(
    dataPoints: List<Pair<String, Double>>,
    label: String,
    modifier: Modifier = Modifier
) {
    val colorPrimary = MaterialTheme.colorScheme.primary.toArgb()
    val colorOnSurface = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(true)
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = colorOnSurface
                    granularity = 1f
                }
                
                axisLeft.apply {
                    setDrawGridLines(true)
                    textColor = colorOnSurface
                }
                
                axisRight.isEnabled = false
                legend.textColor = colorOnSurface
            }
        },
        update = { chart ->
            val entries = dataPoints.mapIndexed { index, pair ->
                Entry(index.toFloat(), pair.second.toFloat())
            }

            val dataSet = LineDataSet(entries, label).apply {
                color = colorPrimary
                setCircleColor(colorPrimary)
                lineWidth = 2f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 10f
                valueTextColor = colorOnSurface
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dataPoints.map { it.first })
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
    )
}
