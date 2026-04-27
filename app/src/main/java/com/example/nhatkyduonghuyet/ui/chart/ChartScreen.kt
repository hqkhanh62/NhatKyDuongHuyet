package com.example.nhatkyduonghuyet.ui.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import com.github.tehras.charts.line.LineChart
import com.github.tehras.charts.line.LineChartData
import com.github.tehras.charts.line.renderer.point.FilledCircularPointDrawer
import com.github.tehras.charts.line.renderer.line.SolidLineDrawer
import com.github.tehras.charts.line.renderer.xaxis.SimpleXAxisDrawer
import com.github.tehras.charts.line.renderer.yaxis.SimpleYAxisDrawer
import com.github.tehras.charts.line.LineChartData.Point

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    navController: NavController,
    viewModel: LogEntryViewModel,
    chartType: String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (chartType == "daily") "Biểu đồ Đường huyết Hàng ngày" else "Biểu đồ Đường huyết Hàng tuần") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().padding(16.dp)) {
            if (chartType == "daily") {
                DailyBloodGlucoseChart(viewModel = viewModel, selectedDate = viewModel.currentDate.value)
            } else if (chartType == "weekly") {
                WeeklyBloodGlucoseChart(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DailyBloodGlucoseChart(viewModel: LogEntryViewModel, selectedDate: String) {
    val dailyData by viewModel.getDailyChartData(selectedDate).collectAsState()

    val points = dailyData.map { Point(it.second.toFloat(), it.first) }
    // Placeholder for daily chart data
    // This will need to be populated from the ViewModel based on selected date


    LineChart(
        lineChartData = LineChartData(
            points = points,
            lineDrawer = SolidLineDrawer(color = Color.Blue)
        ),
        modifier = Modifier.fillMaxWidth().height(250.dp),
        pointDrawer = FilledCircularPointDrawer(),
        xAxisDrawer = SimpleXAxisDrawer(),
        yAxisDrawer = SimpleYAxisDrawer()
    )
}

@Composable
fun WeeklyBloodGlucoseChart(viewModel: LogEntryViewModel) {
    val weeklyData by viewModel.getWeeklyChartData().collectAsState()

    val points = weeklyData.map { Point(it.second.toFloat(), it.first) }
    // Placeholder for weekly chart data
    // This will need to be populated from the ViewModel


    LineChart(
        lineChartData = LineChartData(
            points = points,
            lineDrawer = SolidLineDrawer(color = Color.Green)
        ),
        modifier = Modifier.fillMaxWidth().height(250.dp),
        pointDrawer = FilledCircularPointDrawer(),
        xAxisDrawer = SimpleXAxisDrawer(),
        yAxisDrawer = SimpleYAxisDrawer()
    )
}
