package com.example.nhatkyduonghuyet.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.ui.chart.DayInfoCard
import com.example.nhatkyduonghuyet.ui.chart.aggregateBySession
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: LogEntryViewModel
) {
    val allEntries by viewModel.allLogEntries.collectAsState()
    
    var categoryFilter by remember { mutableStateOf("Tất cả") }
    var valueFilterType by remember { mutableStateOf("Tất cả") }
    var specificValue by remember { mutableStateOf("") }
    var fromValue by remember { mutableStateOf("") }
    var toValue by remember { mutableStateOf("") }

    val categories = listOf("Tất cả", "Trước ăn", "Sau ăn", "Sáng", "Trưa", "Chiều", "Tối")
    val valueTypes = listOf("Tất cả", "Max", "Min", "TB", "Từ - Đến", "Lớn hơn hoặc bằng", "Nhỏ hơn hoặc bằng", "Giá trị cụ thể")

    val filteredResults = remember(allEntries, categoryFilter, valueFilterType, specificValue, fromValue, toValue) {
        var result = allEntries

        // 1. Category Filter
        result = when (categoryFilter) {
            "Trước ăn" -> result.filter { it.bgBefore != null }
            "Sau ăn" -> result.filter { it.bgAfter != null }
            "Sáng" -> result.filter { it.session == "Sáng" }
            "Trưa" -> result.filter { it.session == "Trưa" }
            "Chiều" -> result.filter { it.session == "Chiều" }
            "Tối" -> result.filter { it.session == "Tối" }
            else -> result
        }

        // 2. Value Filter logic
        val sessionPoints = aggregateBySession(result)
        
        when (valueFilterType) {
            "Max" -> {
                // Find the absolute maximum individual reading in the current category/result set
                val rawValues = result.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
                val absoluteMax = rawValues.maxOrNull()
                if (absoluteMax != null) {
                    // Find all dates that have this absolute maximum reading
                    val datesWithMax = result.filter { it.bgBefore == absoluteMax || it.bgAfter == absoluteMax }
                        .map { it.date }
                        .distinct()
                    sessionPoints.filter { it.fullDate in datesWithMax }
                } else emptyList()
            }
            "Min" -> {
                // Find the absolute minimum individual reading
                val rawValues = result.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
                val absoluteMin = rawValues.minOrNull()
                if (absoluteMin != null) {
                    val datesWithMin = result.filter { it.bgBefore == absoluteMin || it.bgAfter == absoluteMin }
                        .map { it.date }
                        .distinct()
                    sessionPoints.filter { it.fullDate in datesWithMin }
                } else emptyList()
            }
            "TB" -> {
                val rawValues = result.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
                val globalAvg = if (rawValues.isEmpty()) 0.0 else rawValues.average()
                // Show days where daily average is close to the global average
                sessionPoints.filter { it.avgDaily != null && kotlin.math.abs(it.avgDaily!! - globalAvg) < 0.5 }
            }
            "Từ - Đến" -> {
                val from = fromValue.toDoubleOrNull() ?: 0.0
                val to = toValue.toDoubleOrNull() ?: Double.MAX_VALUE
                sessionPoints.filter { it.avgDaily != null && it.avgDaily!! >= from && it.avgDaily!! <= to }
            }
            "Lớn hơn hoặc bằng" -> {
                val spec = specificValue.toDoubleOrNull()
                if (spec != null) {
                    sessionPoints.filter { it.avgDaily != null && it.avgDaily!! >= spec }
                } else sessionPoints
            }
            "Nhỏ hơn hoặc bằng" -> {
                val spec = specificValue.toDoubleOrNull()
                if (spec != null) {
                    sessionPoints.filter { it.avgDaily != null && it.avgDaily!! <= spec }
                } else sessionPoints
            }
            "Giá trị cụ thể" -> {
                val spec = specificValue.toDoubleOrNull()
                if (spec != null) {
                    sessionPoints.filter { it.avgDaily != null && kotlin.math.abs(it.avgDaily!! - spec) < 0.2 }
                } else sessionPoints
            }
            else -> sessionPoints
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tìm kiếm nâng cao") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            FilterDropdown(label = "Hạng mục", selected = categoryFilter, options = categories) { categoryFilter = it }
            
            Spacer(modifier = Modifier.height(12.dp))

            FilterDropdown(label = "Lọc giá trị", selected = valueFilterType, options = valueTypes) { valueFilterType = it }

            Spacer(modifier = Modifier.height(12.dp))

            if (valueFilterType in listOf("Giá trị cụ thể", "Lớn hơn hoặc bằng", "Nhỏ hơn hoặc bằng")) {
                OutlinedTextField(
                    value = specificValue,
                    onValueChange = { specificValue = it },
                    label = { Text("Nhập giá trị (mmol/L)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            } else if (valueFilterType == "Từ - Đến") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fromValue,
                        onValueChange = { fromValue = it },
                        label = { Text("Từ") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = toValue,
                        onValueChange = { toValue = it },
                        label = { Text("Đến") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Kết quả: ${filteredResults.size} ngày",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredResults) { point ->
                    DayInfoCard(point)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FilterDropdown(label = "Hạng mục", selected = "Tất cả", options = listOf("Tất cả")) {}
            Spacer(modifier = Modifier.height(12.dp))
            FilterDropdown(label = "Lọc giá trị", selected = "Lớn hơn hoặc bằng", options = listOf("Lớn hơn hoặc bằng")) {}
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = "10.0",
                onValueChange = {},
                label = { Text("Nhập giá trị (mmol/L)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
