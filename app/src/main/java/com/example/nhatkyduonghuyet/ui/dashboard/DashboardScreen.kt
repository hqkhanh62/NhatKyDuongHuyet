package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nhatkyduonghuyet.ui.dashboard.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenPro(
    onViewDetails: () -> Unit,
    onNavigateToPrediction: () -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Dashboard Sức Khỏe Pro", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Lọc thời gian")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DashboardTimeFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.label) },
                                    onClick = {
                                        viewModel.setTimeFilter(filter)
                                        showFilterMenu = false
                                    },
                                    trailingIcon = {
                                        if (state.currentFilter == filter) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Quét", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToPrediction) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "Dự đoán", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onViewDetails) {
                        Icon(imageVector = Icons.Default.List, contentDescription = "Danh sách")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Đang xem: ${state.currentFilter.label}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            item {
                AnimatedStatRow(state)
            }

            item {
                RealtimePredictionCard(state.realtimePrediction)
            }

            item {
                GlucoseChartPro(state)
            }

            item {
                RiskSummaryCard(state)
            }

            item {
                Text("Phân tích thông minh", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                InsightList(state.insights)
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardProPreview() {
    MaterialTheme {
        DashboardScreenPro(onViewDetails = {}, onNavigateToPrediction = {}, onNavigateToScanner = {})
    }
}
