package com.example.nhatkyduonghuyet.ui.chart

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChartScreen(
    navController: NavController,
    viewModel: LogEntryViewModel,
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val allEntries by viewModel.allLogEntries.collectAsState()
    val context = LocalContext.current

    var showMorning by remember { mutableStateOf(true) }
    var showNoon by remember { mutableStateOf(true) }
    var showEvening by remember { mutableStateOf(true) }
    var showNight by remember { mutableStateOf(false) }
    var showDailyAvg by remember { mutableStateOf(false) }

    val dailyPoints = remember(allEntries) {
        aggregateBySession(allEntries)
    }

    var startIdxState by remember { mutableStateOf(0f) }
    var endIdxState by remember { mutableStateOf(100f) }

    val visiblePoints = remember(dailyPoints, startIdxState, endIdxState) {
        val count = dailyPoints.size
        if (count == 0) return@remember emptyList<SessionPoint>()
        var s = startIdxState.toInt()
        if (s < 0) s = 0
        if (s >= count) s = count - 1
        var e = endIdxState.toInt()
        if (e < 0) e = 0
        if (e >= count) e = count - 1
        if (s <= e) dailyPoints.subList(s, if (e + 1 > count) count else e + 1) else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phân tích đường huyết (mmol/L)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { dashboardViewModel.exportToPdf(context) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Xuất PDF")
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
            ) {
                if (dailyPoints.isEmpty()) {
                    Text(
                        text = "Chưa có dữ liệu",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        factory = { context ->
                            val chart = LineChart(context)
                            chart.description.isEnabled = false
                            chart.setTouchEnabled(true)
                            chart.isDragEnabled = true
                            chart.isScaleXEnabled = true
                            chart.isScaleYEnabled = false
                            chart.setPinchZoom(true)
                            
                            val xAxis = chart.xAxis
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.setDrawGridLines(false)
                            xAxis.granularity = 1f
                            xAxis.labelRotationAngle = -45f
                            xAxis.valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    val i = value.toInt()
                                    return if (i >= 0 && i < dailyPoints.size) dailyPoints[i].dateLabel else ""
                                }
                            }
                            
                            chart.axisRight.isEnabled = false
                            val yAxis = chart.axisLeft
                            yAxis.setDrawGridLines(true)
                            yAxis.axisMinimum = 3f
                            yAxis.axisMaximum = 15f
                            
                            chart.legend.isEnabled = true
                            
                            chart.onChartGestureListener = object : OnChartGestureListener {
                                private fun update() {
                                    startIdxState = chart.lowestVisibleX
                                    endIdxState = chart.highestVisibleX
                                }
                                override fun onChartGestureStart(me: MotionEvent?, lastGesture: ChartTouchListener.ChartGesture?) {}
                                override fun onChartGestureEnd(me: MotionEvent?, lastGesture: ChartTouchListener.ChartGesture?) { update() }
                                override fun onChartLongPressed(me: MotionEvent?) {}
                                override fun onChartDoubleTapped(me: MotionEvent?) {}
                                override fun onChartSingleTapped(me: MotionEvent?) {}
                                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) { update() }
                                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) { update() }
                                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) { update() }
                            }
                            chart
                        },
                        update = { chart ->
                            val dataSets = mutableListOf<ILineDataSet>()
                            
                            fun createDS(vList: List<Double?>, label: String, color: Color, isDashed: Boolean = false): LineDataSet? {
                                val entries = mutableListOf<Entry>()
                                for (i in 0 until vList.size) {
                                    val v = vList[i]
                                    if (v != null) entries.add(Entry(i.toFloat(), v.toFloat()))
                                }
                                if (entries.isEmpty()) return null
                                return LineDataSet(entries, label).apply {
                                    this.color = color.toArgb()
                                    setCircleColor(color.toArgb())
                                    lineWidth = 2.5f
                                    circleRadius = 3.5f
                                    setDrawCircleHole(false)
                                    setDrawValues(false)
                                    mode = LineDataSet.Mode.CUBIC_BEZIER
                                    if (isDashed) enableDashedLine(20f, 10f, 0f)
                                }
                            }

                            if (showMorning) createDS(dailyPoints.map { it.avgMorning }, "Sáng", Color(0xFF2196F3))?.let { dataSets.add(it) }
                            if (showNoon) createDS(dailyPoints.map { it.avgNoon }, "Trưa", Color(0xFFFF9800))?.let { dataSets.add(it) }
                            if (showEvening) createDS(dailyPoints.map { it.avgEvening }, "Chiều", Color(0xFFF44336))?.let { dataSets.add(it) }
                            if (showNight) createDS(dailyPoints.map { it.avgNight }, "Tối", Color(0xFF9C27B0))?.let { dataSets.add(it) }
                            if (showDailyAvg) createDS(dailyPoints.map { it.avgDaily }, "TB Ngày", Color(0xFF4CAF50), true)?.let { dataSets.add(it) }

                            if (dataSets.isNotEmpty()) {
                                chart.data = LineData(dataSets)
                                chart.invalidate()
                            } else {
                                chart.clear()
                            }
                            
                            chart.post {
                                startIdxState = chart.lowestVisibleX
                                endIdxState = chart.highestVisibleX
                            }
                        }
                    )
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
                    text = "TB Ngày: ${String.format(Locale.getDefault(), "%.1f", day.avgDaily ?: 0.0)}",
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
            text = if (value == null) "-" else String.format(Locale.getDefault(), "%.1f", value),
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
    
    val grouped = entries.groupBy { it.date }
    val keysList = grouped.keys.toList()
    val sortedDates = keysList.sorted()
    
    val result = mutableListOf<SessionPoint>()

    for (date in sortedDates) {
        val list = grouped[date] ?: continue
        
        fun getSessionAvg(session: String): Double? {
            val sVals = mutableListOf<Double>()
            for (entry in list) {
                if (entry.session == session) {
                    val b = entry.bgBefore
                    if (b != null) sVals.add(b)
                    val a = entry.bgAfter
                    if (a != null) sVals.add(a)
                }
            }
            if (sVals.isEmpty()) return null
            var sum = 0.0
            for (v in sVals) sum += v
            return sum / sVals.size
        }
        val parsedDate = try { inputFormat.parse(date) } catch (e: Exception) { null }
        
        val dailyVals = mutableListOf<Double>()
        for (entry in list) {
            val b = entry.bgBefore
            if (b != null) dailyVals.add(b)
            val a = entry.bgAfter
            if (a != null) dailyVals.add(a)
        }
        
        var dAvgVal: Double? = null
        // Rule: skip average calculation if only one measurement exists
        if (dailyVals.size > 1) {
            var sum = 0.0
            for (v in dailyVals) sum += v
            dAvgVal = sum / dailyVals.size
        }

        result.add(SessionPoint(
            fullDate = date,
            dateLabel = if (parsedDate != null) outputFormat.format(parsedDate) else date,
            avgMorning = getSessionAvg("Sáng"),
            avgNoon = getSessionAvg("Trưa"),
            avgEvening = getSessionAvg("Chiều"),
            avgNight = getSessionAvg("Tối"),
            avgDaily = dAvgVal
        ))
    }
    return result
}
