package com.example.nhatkyduonghuyet.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nhatkyduonghuyet.ui.components.LineChartV2
import com.example.nhatkyduonghuyet.ui.components.DonutChart
import com.example.nhatkyduonghuyet.ui.components.DonutChartData
import com.example.nhatkyduonghuyet.viewmodel.MultiSeriesPoint
import com.example.nhatkyduonghuyet.viewmodel.StatsViewModel
import com.example.nhatkyduonghuyet.viewmodel.TimeFilter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.delay
import java.util.Locale

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
    val aiState by viewModel.aiState.collectAsState()
    
    val showBefore by viewModel.showBefore.collectAsState()
    val showAfter by viewModel.showAfter.collectAsState()
    val showDaily by viewModel.showDaily.collectAsState()

    var selectedX by remember { mutableStateOf<Float?>(null) }

    // Auto AI Update loop
    LaunchedEffect(Unit) {
        while (true) {
            val morningInput = floatArrayOf(5.0f, 5.3f, 5.8f, 6.1f, 5.7f)
            val afternoonInput = floatArrayOf(7.8f, 8.2f, 9.1f, 8.5f, 8.0f)
            viewModel.updatePredictions(morningInput, afternoonInput)
            delay(5000)
        }
    }

    val donutData = remember(stats) {
        listOf(
            DonutChartData("Vùng thấp (<4.0)", stats.lowCount, stats.lowPercent, Color(0xFF2196F3)),
            DonutChartData("Bình thường (4.0-7.0)", stats.normalCount, stats.normalPercent, Color(0xFF4CAF50)),
            DonutChartData("Tiền tiểu đường (7.0-11.0)", stats.preCount, stats.prePercent, Color(0xFFFFC107)),
            DonutChartData("Vùng cao (>11.0)", stats.highCount, stats.highPercent, Color(0xFFF44336))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D47A1), Color(0xFF1E88E5), Color(0xFFF5F5F5))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Glucose AI Monitor", 
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        ) 
                    },
                    actions = {
                        IconButton(onClick = onViewDetails) {
                            Icon(Icons.Default.List, contentDescription = "Danh sách", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    // Force equal height using IntrinsicSize.Min
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GlucoseAiCard(
                            label = "SÁNG (Trước ăn)",
                            prediction = aiState.morningPrediction,
                            risk = aiState.morningRisk,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        GlucoseAiCard(
                            label = "Chiều (Trước ngủ)",
                            prediction = aiState.afternoonPrediction,
                            risk = aiState.afternoonRisk,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }

                item {
                    PremiumChartSection(chartData, selectedX) { selectedX = it }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Health Overview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black.copy(alpha = 0.7f)
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Xu hướng chi tiết",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ChartToggleRow("TB Trước ăn", showBefore, Color(0xFF2196F3)) { viewModel.toggleBefore() }
                                ChartToggleRow("TB Sau ăn", showAfter, Color(0xFFF44336)) { viewModel.toggleAfter() }
                                ChartToggleRow("Trung bình ngày", showDaily, Color(0xFF4CAF50)) { viewModel.toggleDaily() }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (chartData.isNotEmpty() && (showBefore || showAfter || showDaily)) {
                                Box(modifier = Modifier.height(180.dp)) {
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
                                        .height(180.dp),
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                    ) {
                        Text("Xem nhật ký chi tiết ($totalCount bản ghi)", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun GlucoseAiCard(label: String, prediction: Float, risk: String, modifier: Modifier = Modifier) {
    val animatedValue by animateFloatAsState(
        targetValue = prediction,
        animationSpec = tween(1000),
        label = "GlucoseAnimation"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF0D47A1), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "${"%.1f".format(prediction)}",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                color = Color(0xFF0D47A1)
            )
            Text("mmol/L", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

            val riskColor = when {
                risk.contains("Low") -> Color(0xFF2196F3)
                risk.contains("High") -> Color(0xFFF44336)
                else -> Color(0xFF4CAF50)
            }

            Surface(
                color = riskColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = risk,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = riskColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PremiumChartSection(chartData: List<MultiSeriesPoint>, selectedX: Float?, onValueSelected: (Float?) -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Real-time Glucose Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.height(150.dp).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(true)
                            setPinchZoom(true)
                            
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                granularity = 1f
                                setDrawLabels(true) 
                            }
                            axisRight.isEnabled = false
                            axisLeft.setDrawGridLines(true)
                            legend.isEnabled = true

                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    onValueSelected(e?.x)
                                }
                                override fun onNothingSelected() {
                                    onValueSelected(null)
                                }
                            })
                        }
                    },
                    update = { chart ->
                        val dataSlice = chartData.takeLast(10)
                        val entries = dataSlice.mapIndexed { i, point ->
                            Entry(i.toFloat(), (point.avgDaily ?: 0.0).toFloat()) 
                        }
                        
                        // Fix for X-Axis Logic: ONLY show label for selected index
                        chart.xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val idx = value.toInt()
                                return if (selectedX != null && Math.abs(value - selectedX) < 0.1f && idx >= 0 && idx < dataSlice.size) {
                                    dataSlice[idx].date.substringAfterLast("-")
                                } else ""
                            }
                        }
                        chart.xAxis.setDrawLabels(selectedX != null)
                        chart.notifyDataSetChanged()
                        chart.invalidate()

                        if (entries.isNotEmpty()) {
                            val dataSet = LineDataSet(entries, "Glucose (mmol/L)").apply {
                                val primaryColor = Color(0xFF0D47A1).toArgb()
                                setColor(primaryColor)
                                valueTextColor = android.graphics.Color.BLACK
                                lineWidth = 3f
                                setDrawCircles(true)
                                setCircleColor(primaryColor)
                                circleRadius = 5f
                                setDrawFilled(true)
                                fillAlpha = 50
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                            }
                            chart.data = LineData(dataSet)
                            chart.invalidate()
                        }
                    }
                )
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
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black.copy(alpha = 0.6f))
        ) {
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
    color: Color = Color(0xFF0D47A1),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 2.dp),
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0D47A1), Color(0xFF1E88E5), Color(0xFFF5F5F5))
                    )
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlucoseAiCard("SÁNG (Trước ăn)", 5.6f, "✅ Normal", Modifier.weight(1f).fillMaxHeight())
                    GlucoseAiCard("Chiều (Trước ngủ)", 8.2f, "⚠ High Sugar", Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}
