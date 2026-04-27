package com.example.nhatkyduonghuyet.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import com.example.nhatkyduonghuyet.ui.navigation.Screen
import com.example.nhatkyduonghuyet.util.CsvExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateListScreen(navController: NavController, viewModel: LogEntryViewModel) {
    val allDates by viewModel.allDates.collectAsState()
    val allLogEntries by viewModel.getAllLogEntries().collectAsState(initial = emptyList())
    val context = LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        uri: Uri? ->
        uri?.let {
            val success = CsvExportHelper.exportLogEntriesToCsv(context, it, allLogEntries)
            if (success) {
                // Show a success message
            } else {
                // Show an error message
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhật ký Đường huyết") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Chart.route + "/daily") }) {
                        Icon(Icons.Filled.Analytics, "Biểu đồ")
                    }
                    IconButton(onClick = { createDocumentLauncher.launch("nhat_ky__duong_huyet_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv") }) {
                        Icon(Icons.Filled.Share, "Xuất CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                navController.navigate(Screen.DayDetail.route + "/$currentDate")
            }) {
                Icon(Icons.Filled.Add, "Thêm ngày mới")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            items(allDates) {
                DateItem(date = it) {
                    navController.navigate(Screen.DayDetail.route + "/$it")
                }
            }
        }
    }
}

@Composable
fun DateItem(date: String, onClick: (String) -> Unit) {
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
