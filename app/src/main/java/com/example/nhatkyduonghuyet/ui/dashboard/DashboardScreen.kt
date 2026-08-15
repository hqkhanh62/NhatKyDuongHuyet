package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.ui.dashboard.components.AnimatedStatRow
import com.example.nhatkyduonghuyet.ui.dashboard.components.GlucoseChartPro
import com.example.nhatkyduonghuyet.ui.dashboard.components.InsightList
import com.example.nhatkyduonghuyet.ui.dashboard.components.RealtimePredictionCard
import com.example.nhatkyduonghuyet.ui.dashboard.components.RiskSummaryCard
import com.example.nhatkyduonghuyet.ui.theme.NhatKyDuongHuyetTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenPro(
    onViewDetails: () -> Unit,
    onNavigateToPrediction: () -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val showRetrain by viewModel.showRetrainDialog.collectAsState()

    DashboardScreenProContent(
        state = state,
        showRetrain = showRetrain,
        onDismissRetrain = viewModel::dismissRetrainDialog,
        onTimeFilterSelected = viewModel::setTimeFilter,
        onRequestCloudInsight = viewModel::requestGeminiAnalysis,
        onViewDetails = onViewDetails,
        onNavigateToPrediction = onNavigateToPrediction,
        onNavigateToScanner = onNavigateToScanner
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenProContent(
    state: DashboardUiState,
    showRetrain: Boolean,
    onDismissRetrain: () -> Unit,
    onTimeFilterSelected: (DashboardTimeFilter) -> Unit,
    onRequestCloudInsight: () -> Unit,
    onViewDetails: () -> Unit,
    onNavigateToPrediction: () -> Unit,
    onNavigateToScanner: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showFilterMenu by remember { mutableStateOf(false) }

    if (showRetrain) {
        AlertDialog(
            onDismissRequest = onDismissRetrain,
            title = { Text("AI Intelligence Upgrade") },
            text = { Text("Bạn đã có thêm 50 dữ liệu mới. Hệ thống đã sẵn sàng để huấn luyện lại mô hình để dự báo chính xác hơn cho riêng bạn.") },
            confirmButton = { Button(onClick = onDismissRetrain) { Text("Tuyệt vời") } }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Dashboard Sức Khỏe Pro", fontWeight = FontWeight.Bold) },
                actions = {
                    FilterMenu(
                        currentFilter = state.currentFilter,
                        expanded = showFilterMenu,
                        onExpandedChange = { showFilterMenu = it },
                        onTimeFilterSelected = onTimeFilterSelected
                    )
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.PhotoCamera, "Quét", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToPrediction) {
                        Icon(Icons.Default.Favorite, "Dự đoán", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onViewDetails) {
                        Icon(Icons.AutoMirrored.Filled.List, "Danh sách")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
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
            item { AnimatedStatRow(state) }
            item {
                RealtimePredictionCard(state.realtimePrediction, state.multiStepForecast)
                state.forecastStatus?.let { ForecastStatusCard(it) }
            }
            item { GlucoseChartPro(state) }
            item { GeminiInsightCard(state.geminiInsight, onRequestCloudInsight) }
            item { RiskSummaryCard(state) }
            item {
                Text("Phân tích thông minh", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                InsightList(state.insights)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FilterMenu(
    currentFilter: DashboardTimeFilter,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTimeFilterSelected: (DashboardTimeFilter) -> Unit
) {
    androidx.compose.foundation.layout.Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Default.DateRange, "Lọc thời gian")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DashboardTimeFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        onTimeFilterSelected(filter)
                        onExpandedChange(false)
                    },
                    trailingIcon = {
                        if (currentFilter == filter) Icon(Icons.Default.Check, null)
                    }
                )
            }
        }
    }
}

@Composable
private fun ForecastStatusCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun GeminiInsightCard(state: GeminiInsightUiState, onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Phân tích AI từ máy chủ",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            when (state) {
                GeminiInsightUiState.Idle -> {
                    Text(
                        "Nhật ký đường huyết chỉ được gửi đi sau khi bạn chọn phân tích.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onRequest) { Text("Phân tích dữ liệu của tôi") }
                }
                GeminiInsightUiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(10.dp))
                        Text("Đang gửi yêu cầu phân tích an toàn…")
                    }
                }
                is GeminiInsightUiState.Content -> {
                    Text(state.text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onRequest) { Text("Làm mới phân tích") }
                }
                is GeminiInsightUiState.Unavailable -> {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onRequest) { Text("Thử lại") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardProPreview() {
    val sampleState = DashboardUiState(
        entries = emptyList<LogEntry>(),
        max = 8.5,
        maxCompare = ComparisonData(0.5, 6.0, false),
        avg = 6.2,
        avgCompare = ComparisonData(-0.3, -4.5, true),
        highRate = 12,
        highRateCompare = ComparisonData(-2.0, -15.0, true),
        hba1c = 6.4,
        hba1cCompare = ComparisonData(0.1, 1.5, false),
        currentPeriodPoints = emptyList(),
        previousPeriodPoints = emptyList(),
        insights = listOf("Đường huyết của bạn đang có xu hướng ổn định hơn."),
        realtimePrediction = PredictionResult(6.2f, 6.5f, 0.3f, "✅ Normal"),
        multiStepForecast = MultiStepResult(listOf(6.5f, 6.8f, 7.2f, 7.0f), 7.5f, 5.5f),
        geminiInsight = GeminiInsightUiState.Content("- Đi bộ nhẹ sau ăn trưa.")
    )

    NhatKyDuongHuyetTheme {
        DashboardScreenProContent(
            state = sampleState,
            showRetrain = false,
            onDismissRetrain = {},
            onTimeFilterSelected = {},
            onRequestCloudInsight = {},
            onViewDetails = {},
            onNavigateToPrediction = {},
            onNavigateToScanner = {}
        )
    }
}