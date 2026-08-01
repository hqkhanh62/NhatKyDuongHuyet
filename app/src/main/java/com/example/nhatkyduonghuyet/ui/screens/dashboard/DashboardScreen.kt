package com.example.nhatkyduonghuyet.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyColumn
import com.example.nhatkyduonghuyet.ui.components.GlucoseChart
import com.example.nhatkyduonghuyet.ui.components.SummaryCard

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
            CenterAlignedTopAppBar(title = { Text("Dashboard Sức Khỏe") })
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dự báo từ AI", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(prediction)
                    }
                }
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
