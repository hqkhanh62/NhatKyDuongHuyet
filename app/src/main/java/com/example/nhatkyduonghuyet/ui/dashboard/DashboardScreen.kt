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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.res.stringResource
import com.example.nhatkyduonghuyet.R
import androidx.compose.material.icons.filled.MedicalServices
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
    onNavigateToMedication: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val showRetrain by viewModel.showRetrainDialog.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    DashboardScreenProContent(
        state = state,
        showRetrain = showRetrain,
        onDismissRetrain = viewModel::dismissRetrainDialog,
        onTimeFilterSelected = viewModel::setTimeFilter,
        onRequestCloudInsight = viewModel::requestGeminiAnalysis,
        onViewDetails = onViewDetails,
        onNavigateToPrediction = onNavigateToPrediction,
        onNavigateToScanner = onNavigateToScanner,
        onNavigateToMedication = onNavigateToMedication,
        onExportPdf = { viewModel.exportToPdf(context) }
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
    onNavigateToScanner: () -> Unit,
    onNavigateToMedication: () -> Unit,
    onExportPdf: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showFilterMenu by remember { mutableStateOf(false) }

    if (showRetrain) {
        AlertDialog(
            onDismissRequest = onDismissRetrain,
            title = { Text(stringResource(R.string.ai_upgrade_title)) },
            text = { Text(stringResource(R.string.ai_upgrade_msg)) },
            confirmButton = { Button(onClick = onDismissRetrain) { Text(stringResource(R.string.great)) } }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.dashboard_title), fontWeight = FontWeight.Bold) },
                actions = {
                    FilterMenu(
                        currentFilter = state.currentFilter,
                        expanded = showFilterMenu,
                        onExpandedChange = { showFilterMenu = it },
                        onTimeFilterSelected = onTimeFilterSelected
                    )
                    IconButton(onClick = onExportPdf) {
                        Icon(Icons.Default.PictureAsPdf, stringResource(R.string.export_report), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToMedication) {
                        Icon(Icons.Default.MedicalServices, stringResource(R.string.medication_title), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.PhotoCamera, stringResource(R.string.scan), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToPrediction) {
                        Icon(Icons.Default.Favorite, stringResource(R.string.predict), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onViewDetails) {
                        Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.list))
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
                        text = stringResource(R.string.viewing_period, state.currentFilter.label),
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
                Text(stringResource(R.string.smart_analysis), style = MaterialTheme.typography.titleMedium)
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
            Icon(Icons.Default.DateRange, stringResource(R.string.time_filter))
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
                    text = stringResource(R.string.ai_analysis_server),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            when (state) {
                GeminiInsightUiState.Idle -> {
                    Text(
                        stringResource(R.string.idle_gemini_msg),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onRequest) { Text(stringResource(R.string.analyze_my_data)) }
                }
                GeminiInsightUiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(R.string.sending_request))
                    }
                }
                is GeminiInsightUiState.Content -> {
                    Text(state.text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onRequest) { Text(stringResource(R.string.refresh_analysis)) }
                }
                is GeminiInsightUiState.Unavailable -> {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onRequest) { Text(stringResource(R.string.try_again)) }
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
            onNavigateToScanner = {},
            onNavigateToMedication = {}
        )
    }
}