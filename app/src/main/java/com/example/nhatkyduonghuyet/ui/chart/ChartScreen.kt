package com.example.nhatkyduonghuyet.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChartScreen(
    navController: NavController,
    viewModel: LogEntryViewModel
) {
    val allEntries by viewModel.allLogEntries.collectAsState()

    var showMorning by remember { mutableStateOf(true) }
    var showNoon by remember { mutableStateOf(true) }
    var showEvening by remember { mutableStateOf(true) }
    var showNight by remember { mutableStateOf(false) }
    var showDailyAvg by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    val chartScrollState = rememberScrollState()
    var containerWidthPx by remember { mutableIntStateOf(0) }

    val dailyPoints = remember(allEntries) {
        aggregateBySession(allEntries)
    }

    // Dynamic calculation of visible points based on scroll and zoom
    val visiblePoints = remember(dailyPoints, chartScrollState.value, zoomScale, containerWidthPx) {
        if (dailyPoints.isEmpty() || containerWidthPx <= 0) return@remember dailyPoints
        
        val totalPoints = dailyPoints.size
        val screenWidth = containerWidthPx.toFloat()
        val contentWidth = screenWidth * zoomScale
        val pointWidth = if (totalPoints > 1) contentWidth / (totalPoints - 1) else screenWidth
        
        val startIdx = floor(chartScrollState.value / pointWidth).toInt().coerceIn(0, totalPoints - 1)
        val endIdx = ceil((chartScrollState.value + screenWidth) / pointWidth).toInt().coerceIn(0, totalPoints - 1)
        
        if (startIdx <= endIdx) dailyPoints.slice(startIdx..endIdx) else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phân tích đường huyết (mmol/L)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(text = "Lọc dữ liệu hiển thị", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SessionFilterChip("Sáng", showMorning, Color(0xFF2196F3)) { showMorning = !showMorning }
                    SessionFilterChip("Trưa", showNoon, Color(0xFFFF9800)) { showNoon = !showNoon }
                    SessionFilterChip("Chiều", showEvening, Color(0xFFF44336)) { showEvening = !showEvening }
                    SessionFilterChip("Tối", showNight, Color(0xFF9C27B0)) { showNight = !showNight }
                    SessionFilterChip("TB Ngày", showDailyAvg, Color(0xFF4CAF50)) { showDailyAvg = !showDailyAvg }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .onGloballyPositioned { containerWidthPx = it.size.width }
            ) {
                if (dailyPoints.isEmpty()) {
                    Text(
                        text = "Chưa có dữ liệu",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(chartScrollState)
                            .transformable(
                                state = rememberTransformableState { zoomChange, _, _ ->
                                    zoomScale = (zoomScale * zoomChange).coerceIn(1f, 10f)
                                }
                            )
                    ) {
                        val density = LocalDensity.current
                        val screenWidthDp = with(density) { containerWidthPx.toDp() }
                        val canvasWidth = if (dailyPoints.size > 1) screenWidthDp * zoomScale else screenWidthDp

                        PremiumFlexibleLineChart(
                            points = dailyPoints,
                            showMorning = showMorning,
                            showNoon = showNoon,
                            showEvening = showEvening,
                            showNight = showNight,
                            showDailyAvg = showDailyAvg,
                            modifier = Modifier
                                .width(canvasWidth)
                                .fillMaxHeight(),
                            minY = 3.0,
                            maxY = 15.0
                        )
                    }
                    
                    if (zoomScale > 1f) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Zoom: ${String.format("%.1f", zoomScale)}x",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Text(
                text = "Dữ liệu vùng hiển thị (${visiblePoints.size} ngày)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(visiblePoints.size) { index ->
                    val day = visiblePoints[visiblePoints.size - 1 - index]
                    DayInfoCard(day)
                }
            }
        }
    }
}

@Composable
fun DayInfoCard(day: SessionPoint) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = day.fullDate, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "TB Ngày: ${String.format("%.1f", day.avgDaily ?: 0.0)}",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat("Sáng", day.avgMorning, Color(0xFF2196F3))
                MiniStat("Trưa", day.avgNoon, Color(0xFFFF9800))
                MiniStat("Chiều", day.avgEvening, Color(0xFFF44336))
                MiniStat("Tối", day.avgNight, Color(0xFF9C27B0))
            }
        }
    }
}

@Composable
fun MiniStat(label: String, value: Double?, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(
            text = if (value == null) "-" else String.format("%.1f", value),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value != null) color else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionFilterChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        )
    )
}

data class SessionPoint(
    val fullDate: String,
    val dateLabel: String,
    val avgMorning: Double?,
    val avgNoon: Double?,
    val avgEvening: Double?,
    val avgNight: Double?,
    val avgDaily: Double?
)

fun aggregateBySession(entries: List<LogEntry>): List<SessionPoint> {
    if (entries.isEmpty()) return emptyList()
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outputFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val byDate = entries.groupBy { it.date }.toSortedMap()
    val result = mutableListOf<SessionPoint>()

    byDate.forEach { (date, list) ->
        fun getSessionAvg(session: String): Double? {
            val vals = list.filter { it.session == session }.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
            return if (vals.isNotEmpty()) vals.average() else null
        }
        val parsedDate = try { inputFormat.parse(date) } catch (e: Exception) { null }
        
        result.add(SessionPoint(
            fullDate = date,
            dateLabel = parsedDate?.let { outputFormat.format(it) } ?: date,
            avgMorning = getSessionAvg("Sáng"),
            avgNoon = getSessionAvg("Trưa"),
            avgEvening = getSessionAvg("Chiều"),
            avgNight = getSessionAvg("Tối"),
            avgDaily = list.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }.let { if (it.isEmpty()) null else it.average() }
        ))
    }
    return result
}

@Composable
fun PremiumFlexibleLineChart(
    points: List<SessionPoint>,
    showMorning: Boolean,
    showNoon: Boolean,
    showEvening: Boolean,
    showNight: Boolean,
    showDailyAvg: Boolean,
    modifier: Modifier = Modifier,
    minY: Double,
    maxY: Double
) {
    val density = LocalDensity.current
    val leftPaddingPx = with(density) { 40.dp.toPx() }
    val bottomPaddingPx = with(density) { 40.dp.toPx() }
    val topPaddingPx = with(density) { 20.dp.toPx() }
    val rightPaddingPx = with(density) { 20.dp.toPx() }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val xStart = leftPaddingPx
        val xEnd = width - rightPaddingPx
        val yTop = topPaddingPx
        val yBottom = height - bottomPaddingPx
        val chartWidth = xEnd - xStart
        val chartHeight = yBottom - yTop

        // Draw Y Axis grid lines
        val yLabels = listOf(3.0, 6.0, 9.0, 12.0, 15.0)
        yLabels.forEach { label ->
            val ratio = ((label - minY) / (maxY - minY)).coerceIn(0.0, 1.0)
            val y = yBottom - (ratio * chartHeight).toFloat()
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(xStart, y),
                end = xEnd.let { Offset(it, y) },
                strokeWidth = 1f
            )
        }

        if (points.size <= 1) return@Canvas
        val stepX = chartWidth / (points.size - 1)

        fun valueToY(value: Double?): Float? {
            if (value == null) return null
            val clamped = value.coerceIn(minY, maxY)
            return (yBottom - ((clamped - minY) / (maxY - minY) * chartHeight).toFloat())
        }

        if (showMorning) drawSmoothLine(points.map { it.avgMorning }, Color(0xFF2196F3), xStart, stepX, ::valueToY)
        if (showNoon) drawSmoothLine(points.map { it.avgNoon }, Color(0xFFFF9800), xStart, stepX, ::valueToY)
        if (showEvening) drawSmoothLine(points.map { it.avgEvening }, Color(0xFFF44336), xStart, stepX, ::valueToY)
        if (showNight) drawSmoothLine(points.map { it.avgNight }, Color(0xFF9C27B0), xStart, stepX, ::valueToY)
        if (showDailyAvg) drawSmoothLine(points.map { it.avgDaily }, Color(0xFF4CAF50), xStart, stepX, ::valueToY, isDashed = true)
    }
}

fun DrawScope.drawSmoothLine(
    values: List<Double?>,
    color: Color,
    xStart: Float,
    stepX: Float,
    valueToY: (Double?) -> Float?,
    isDashed: Boolean = false
) {
    val path = Path()
    var firstPoint = true

    values.forEachIndexed { index, value ->
        val x = xStart + stepX * index
        val y = valueToY(value)
        if (y != null) {
            if (firstPoint) {
                path.moveTo(x, y)
                firstPoint = false
            } else {
                path.lineTo(x, y)
            }
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
        } else {
            firstPoint = true
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 3.dp.toPx(),
            pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f) else null
        )
    )
}
