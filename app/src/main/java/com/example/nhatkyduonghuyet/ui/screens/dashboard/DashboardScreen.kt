package com.example.nhatkyduonghuyet.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nhatkyduonghuyet.ui.components.GlucoseChart
import com.example.nhatkyduonghuyet.ui.components.SummaryCard
import com.example.nhatkyduonghuyet.ui.dashboard.components.RealtimePredictionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onViewDetails: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val prediction by viewModel.prediction.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Dashboard Sức Khỏe Pro") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlucoseChart(logs)
            }

            item {
                SummaryCard(logs)
            }

            item {
                // Hiển thị thẻ dự báo LSTM thời gian thực
                RealtimePredictionCard(
                    prediction = prediction,
                    multiStep = null // Bạn có thể bổ sung multi-step UseCase sau này
                )
            }

            item {
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xem thống kê chi tiết")
                }
            }
        }
    }
}
