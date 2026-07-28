package com.example.nhatkyduonghuyet.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.ui.components.LineChartV2
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import java.text.SimpleDateFormat
import java.util.Locale


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

    val visiblePoints = dailyPoints // Vico handles scrolling/visibility internally if needed, or we keep simple for list


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phân tích đường huyết") },
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
                    .padding(8.dp)
            ) {
                if (dailyPoints.isEmpty()) {
                    Text(
                        text = "Chưa có dữ liệu",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                } else {
                    LineChartV2(
                        points = dailyPoints,
                        showMorning = showMorning,
                        showNoon = showNoon,
                        showEvening = showEvening,
                        showNight = showNight,
                        showDailyAvg = showDailyAvg,
                        modifier = Modifier.fillMaxSize()
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
                    text = "TB Ngày: ${"%.1f".format(day.avgDaily ?: 0.0)}",
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
            text = if (value == null) "-" else "%.1f".format(value),
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

