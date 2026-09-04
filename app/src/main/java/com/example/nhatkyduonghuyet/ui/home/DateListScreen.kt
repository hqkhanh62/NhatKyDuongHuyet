package com.example.nhatkyduonghuyet.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.ui.navigation.GlucoseScreen
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateListScreen(
    navController: NavController,
    viewModel: LogEntryViewModel
) {
    val allDates by viewModel.allDates.collectAsState()

    val showDatePicker = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhật ký Đường huyết") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Biểu đồ") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(GlucoseScreen.Chart.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tìm kiếm") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(GlucoseScreen.Search.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Dự đoán AI") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(GlucoseScreen.Prediction.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sao lưu & Khôi phục") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(GlucoseScreen.Backup.route)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showDatePicker.value = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Thêm ngày mới"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            items(allDates) { date ->
                DateItem(
                    date = date,
                    onClick = { clickedDate ->
                        navController.navigate(GlucoseScreen.DayDetail.route + "/$clickedDate")
                    }
                )
            }
        }
    }

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val formatted = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            ).format(Date(millis))
                            showDatePicker.value = false
                            navController.navigate(GlucoseScreen.DayDetail.route + "/$formatted")
                        } else {
                            showDatePicker.value = false
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text("Huỷ")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun DateItem(
    date: String,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick(date) }
    ) {
        Text(
            text = date,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
