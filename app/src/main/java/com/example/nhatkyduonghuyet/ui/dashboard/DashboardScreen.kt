package com.example.nhatkyduonghuyet.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nhatkyduonghuyet.ui.dashboard.components.*
import com.example.nhatkyduonghuyet.ui.theme.NhatKyDuongHuyetTheme
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.ai.MultiStepResult
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment

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
        onDismissRetrain = { viewModel.dismissRetrainDialog() },
        onTimeFilterSelected = { viewModel.setTimeFilter(it) },
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
            confirmButton = {
                Button(onClick = onDismissRetrain) {
                    Text("Tuyệt vời")
                }
            }
        )
    }

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
                                        onTimeFilterSelected(filter)
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "Danh sách")
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
                RealtimePredictionCard(state.realtimePrediction, state.multiStepForecast)
            }

            item {
                GlucoseChartPro(state)
            }

            item {
                GeminiInsightCard(state.geminiInsight)
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

@Composable
fun GeminiInsightCard(insight: String?) {
    AnimatedVisibility(
        visible = insight != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lời khuyên từ chuyên gia AI (Gemini)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = insight ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
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
        currentPeriodPoints = emptyList<ChartPointPro>(),
        previousPeriodPoints = emptyList<ChartPointPro>(),
        insights = listOf(
            "Đường huyết của bạn đang có xu hướng ổn định hơn.",
            "Cần chú ý lượng Carb trong bữa tối."
        ),
        currentFilter = DashboardTimeFilter.LAST_15_DAYS,
        realtimePrediction = PredictionResult(
            current = 6.2f,
            next = 6.5f,
            trend = 0.3f,
            risk = "Low"
        ),
        multiStepForecast = MultiStepResult(
            hourlyForecasts = listOf(6.5f, 6.8f, 7.2f, 7.0f, 6.7f),
            maxExpected = 7.5f,
            minExpected = 5.5f
        ),
        geminiInsight = "1. Bạn nên tăng cường rau xanh.\n2. Đi bộ nhẹ sau ăn trưa.\n3. Chú ý chỉ số lúc sáng sớm."
    )

    NhatKyDuongHuyetTheme {
        DashboardScreenProContent(
            state = sampleState,
            showRetrain = false,
            onDismissRetrain = {},
            onTimeFilterSelected = {},
            onViewDetails = {},
            onNavigateToPrediction = {},
            onNavigateToScanner = {}
        )
    }
}
