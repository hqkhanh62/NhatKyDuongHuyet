package com.example.nhatkyduonghuyet.ui.screens.stats

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nhatkyduonghuyet.viewmodel.StatsViewModel
import com.example.nhatkyduonghuyet.ui.components.LineChartV2
import com.example.nhatkyduonghuyet.ui.screens.dashboard.StatCard

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val total by viewModel.totalCount.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val chartData by viewModel.chartData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("📊 Thống kê chi tiết", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        StatCard("Tổng bản ghi", total.toString())
        StatCard("Trung bình", "%.1f mmol/L".format(stats.avg))
        StatCard("HbA1c ước tính", "%.1f %%".format(stats.hba1c))

        Spacer(Modifier.height(16.dp))
		
        if (stats.highPercent > 30) {
            Text("🚨 Đường huyết cao nhiều!", color = Color.Red)
        }

        if (stats.lowPercent > 20) {
            Text("⚠️ Nguy cơ hạ đường huyết!", color = Color.Blue)
        }
        Spacer(Modifier.height(20.dp))

        Text("📈 Biểu đồ", style = MaterialTheme.typography.titleLarge)

        Box(modifier = Modifier.height(250.dp)) {
            LineChartV2(chartData)
        }
    }
}
