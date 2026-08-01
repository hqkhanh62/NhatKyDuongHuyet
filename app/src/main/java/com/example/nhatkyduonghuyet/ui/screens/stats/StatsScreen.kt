package com.example.nhatkyduonghuyet.ui.screens.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nhatkyduonghuyet.ui.components.StatItem
import com.example.nhatkyduonghuyet.viewmodel.StatsViewModel

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val total by viewModel.totalCount.collectAsState()
    val stats by viewModel.stats.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Thống kê tổng quát", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        StatItem("Tổng số bản ghi", total.toString())
        StatItem("Trung bình toàn thời gian", "%.1f mmol/L".format(stats.avg))
        StatItem("Cao nhất", "%.1f mmol/L".format(stats.max))
        StatItem("HbA1c ước tính", "%.1f %%".format(stats.hba1c))
    }
}
