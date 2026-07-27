package com.example.nhatkyduonghuyet.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nhatkyduonghuyet.ui.components.LineChartV2
import com.example.nhatkyduonghuyet.ui.components.DonutChart
import com.example.nhatkyduonghuyet.ui.components.DonutChartData
import com.example.nhatkyduonghuyet.viewmodel.StatsViewModel
import com.example.nhatkyduonghuyet.viewmodel.TimeFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onViewDetails: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    
    val showBefore by viewModel.showBefore.collectAsState()
    val showAfter by viewModel.showAfter.collectAsState()
    val showDaily by viewModel.showDaily.collectAsState()

    val donutData = remember(stats) {
        listOf(
            DonutChartData("Vùng thấp (<4.0)", stats.lowCount, stats.lowPercent, Color(0xFF2196F3)),
            DonutChartData("Bình thường (4.0-7.0)", stats.normalCount, stats.normalPercent, Color(0xFF4CAF50)),
            DonutChartData("Tiền tiểu đường (7.0-11.0)", stats.preCount, stats.prePercent, Color(0xFFFFC107)),
            DonutChartData("Vùng cao (>11.0)", stats.highCount, stats.highPercent, Color(0xFFF44336))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onViewDetails) {
                        Icon(Icons.Default.List, contentDescription = "Danh sách")
                    }
                }
            )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tổng quan sức khỏe",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TimeFilterDropdown(
                        currentFilter = timeFilter,
                        onFilterSelected = { viewModel.setTimeFilter(it) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Trung bình",
                        value = String.format("%.1f", stats.avg),
                        unit = "mmol/L",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "HbA1c ước tính",
                        value = String.format("%.1f", stats.hba1c),
                        unit = "%",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF9C27B0)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Thấp nhất",
                        value = String.format("%.1f", stats.min),
                        unit = "mmol/L",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF2196F3)
                    )
                    StatCard(
                        title = "Cao nhất",
                        value = String.format("%.1f", stats.max),
                        unit = "mmol/L",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFF44336)
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Phân bổ đường huyết",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DonutChart(
                            data = donutData,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Xu hướng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Checkbox filters
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ChartToggleRow("TB Trước ăn", showBefore, Color(0xFF2196F3)) { viewModel.toggleBefore() }
                            ChartToggleRow("TB Sau ăn", showAfter, Color(0xFFF44336)) { viewModel.toggleAfter() }
                            ChartToggleRow("Trung bình ngày", showDaily, Color(0xFF4CAF50)) { viewModel.toggleDaily() }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (chartData.isNotEmpty() && (showBefore || showAfter || showDaily)) {
                            Box(modifier = Modifier.height(240.dp)) {
                                LineChartV2(
                                    data = chartData,
                                    showBefore = showBefore,
                                    showAfter = showAfter,
                                    showDaily = showDaily
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (chartData.isEmpty()) "Chưa có đủ dữ liệu" else "Chọn ít nhất 1 đường để hiển thị",
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xem nhật ký chi tiết ($totalCount bản ghi)")
                }
            }
        }
    }
}

@Composable
fun ChartToggleRow(
    label: String,
    checked: Boolean,
    color: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = color,
                uncheckedColor = color.copy(alpha = 0.4f)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (checked) color else Color.Gray,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TimeFilterDropdown(
    currentFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(currentFilter.label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimeFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    unit: String = "",
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}
