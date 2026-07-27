package com.example.nhatkyduonghuyet.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

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

    val dailyPoints = remember(allEntries) {
        aggregateBySession(allEntries)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biểu đồ đường huyết") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (dailyPoints.isEmpty()) {
                Text(
                    text = "Chưa có dữ liệu để vẽ biểu đồ.\nHãy thêm bản ghi trong vài ngày.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                )
            } else {
                Text(
                    text = "Lọc dữ liệu hiển thị",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Filters
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Biểu đồ trung bình theo ngày",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlexibleLineChart(
                    points = dailyPoints,
                    showMorning = showMorning,
                    showNoon = showNoon,
                    showEvening = showEvening,
                    showNight = showNight,
                    showDailyAvg = showDailyAvg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    minY = 3.0,
                    maxY = 15.0,
                    yStep = 2.0
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Chi tiết các chỉ số",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                dailyPoints.reversed().forEach { day ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = day.fullDate, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                InfoText("Sáng", day.avgMorning, Color(0xFF2196F3))
                                InfoText("Trưa", day.avgNoon, Color(0xFFFF9800))
                                InfoText("Chiều", day.avgEvening, Color(0xFFF44336))
                                InfoText("Tối", day.avgNight, Color(0xFF9C27B0))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Trung bình ngày: ${formatDouble(day.avgDaily)} mmol/L",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
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
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color,
            labelColor = Color.Gray
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (selected) color else Color.LightGray,
            borderWidth = 1.dp,
            enabled = true,
            selected = selected
        )
    )
}

@Composable
fun InfoText(label: String, value: Double?, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(
            text = formatDouble(value),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value != null) color else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
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

    val byDate = entries.groupBy { it.date }

    return byDate.toSortedMap().map { (date, list) ->
        fun getSessionAvg(session: String): Double? {
            val sessionList = list.filter { it.session == session }
            val values = sessionList.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
            return if (values.isNotEmpty()) values.average() else null
        }

        val parsedDate = try { inputFormat.parse(date) } catch (e: Exception) { null }
        
        SessionPoint(
            fullDate = date,
            dateLabel = parsedDate?.let { outputFormat.format(it) } ?: date,
            avgMorning = getSessionAvg("Sáng"),
            avgNoon = getSessionAvg("Trưa"),
            avgEvening = getSessionAvg("Chiều"),
            avgNight = getSessionAvg("Tối"),
            avgDaily = list.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }.let { if (it.isEmpty()) null else it.average() }
        )
    }
}

@Composable
fun FlexibleLineChart(
    points: List<SessionPoint>,
    showMorning: Boolean,
    showNoon: Boolean,
    showEvening: Boolean,
    showNight: Boolean,
    showDailyAvg: Boolean,
    modifier: Modifier = Modifier,
    minY: Double,
    maxY: Double,
    yStep: Double
) {
    val axisColor = Color.Gray
    val textColor = MaterialTheme.colorScheme.onSurface

    val leftPadding: Dp = 40.dp
    val bottomPadding: Dp = 32.dp
    val topPadding: Dp = 16.dp
    val rightPadding: Dp = 16.dp

    Column(modifier = modifier.padding(8.dp)) {
        Text(
            text = "mmol/L",
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(start = 4.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height

            val xStart = leftPadding.toPx()
            val xEnd = width - rightPadding.toPx()
            val yTop = topPadding.toPx()
            val yBottom = height - bottomPadding.toPx()

            val chartWidth = xEnd - xStart
            val chartHeight = yBottom - yTop

            // Draw axis
            drawLine(color = axisColor, start = Offset(xStart, yBottom), end = Offset(xEnd, yBottom), strokeWidth = 2f)
            drawLine(color = axisColor, start = Offset(xStart, yTop), end = Offset(xStart, yBottom), strokeWidth = 2f)

            // Grid lines
            var yValue = minY
            while (yValue <= maxY + 0.0001) {
                val ratio = ((yValue - minY) / (maxY - minY)).coerceIn(0.0, 1.0)
                val y = yBottom - (ratio * chartHeight).toFloat()
                drawLine(color = axisColor.copy(alpha = 0.2f), start = Offset(xStart, y), end = Offset(xEnd, y), strokeWidth = 1f)
                yValue += yStep
            }

            if (points.size <= 1) return@Canvas

            val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

            fun valueToY(value: Double?): Float? {
                if (value == null) return null
                val clamped = value.coerceIn(minY, maxY)
                val ratio = (clamped - minY) / (maxY - minY)
                return (yBottom - (ratio * chartHeight).toFloat())
            }

            // Draw Lines
            if (showMorning) drawDataLine(points.map { it.avgMorning }, Color(0xFF2196F3), xStart, stepX, ::valueToY)
            if (showNoon) drawDataLine(points.map { it.avgNoon }, Color(0xFFFF9800), xStart, stepX, ::valueToY)
            if (showEvening) drawDataLine(points.map { it.avgEvening }, Color(0xFFF44336), xStart, stepX, ::valueToY)
            if (showNight) drawDataLine(points.map { it.avgNight }, Color(0xFF9C27B0), xStart, stepX, ::valueToY)
            if (showDailyAvg) drawDataLine(points.map { it.avgDaily }, Color(0xFF4CAF50), xStart, stepX, ::valueToY, isDashed = true)
        }

        // X Axis labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, point ->
                val label = if (points.size > 6 && index % 2 == 1) "" else point.dateLabel
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDataLine(
    values: List<Double?>,
    color: Color,
    xStart: Float,
    stepX: Float,
    valueToY: (Double?) -> Float?,
    isDashed: Boolean = false
) {
    var prevOffset: Offset? = null
    values.forEachIndexed { index, value ->
        val x = xStart + stepX * index
        val y = valueToY(value)
        if (y != null) {
            val current = Offset(x, y)
            prevOffset?.let { prev ->
                drawLine(
                    color = color,
                    start = prev,
                    end = current,
                    strokeWidth = if (isDashed) 3f else 4f,
                    pathEffect = if (isDashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                )
            }
            drawCircle(color = color, radius = 5f, center = current)
            prevOffset = current
        } else {
            prevOffset = null // Break line if data is missing
        }
    }
}

private fun formatDouble(value: Double?): String =
    if (value == null) "-" else ((value * 10).roundToInt() / 10.0).toString()
