package com.example.nhatkyduonghuyet.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.R
import com.example.nhatkyduonghuyet.viewmodel.MedicationUiState
import com.example.nhatkyduonghuyet.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    navController: NavController,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val medications by viewModel.medicationList.collectAsState(initial = emptyList())
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    viewModel.importCsv(content)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.medication_title), fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { filePickerLauncher.launch("text/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.import_csv))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                Column(modifier = Modifier.width(850.dp)) {
                    MedicationTableHeader()
                    
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(medications) { item ->
                            MedicationRow(
                                item = item, 
                                onToggle = { session, taken -> viewModel.toggleMedication(item.medication, session, taken) }
                            )
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(text = stringResource(R.string.medication_name), weight = 2.5f, isHeader = true)
        TableCell(text = stringResource(R.string.medication_dosage), weight = 1.2f, isHeader = true)
        TableCell(text = "Liều dùng / Ghi chú", weight = 3f, isHeader = true)
        
        // Session Checkboxes Header
        val sessions = listOf("Sáng", "Trưa", "Chiều", "Tối", "Ngủ")
        sessions.forEach {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(it, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), fontSize = 12.sp)
            }
        }
        
        TableCell(text = stringResource(R.string.medication_monthly_count), weight = 1.2f, isHeader = true)
    }
}

@Composable
fun MedicationRow(item: MedicationUiState, onToggle: (String, Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(text = item.medication.name, weight = 2.5f)
        TableCell(text = item.medication.dosage, weight = 1.2f)
        
        val instructionText = buildString {
            append(item.medication.instruction)
            if (item.medication.timing.isNotBlank()) {
                append(" (${item.medication.timing})")
            }
        }
        TableCell(text = instructionText, weight = 3f)
        
        // Checkboxes
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Checkbox(checked = item.isTakenMorning, onCheckedChange = { onToggle("MORNING", it) })
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Checkbox(checked = item.isTakenNoon, onCheckedChange = { onToggle("NOON", it) })
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Checkbox(checked = item.isTakenAfternoon, onCheckedChange = { onToggle("AFTERNOON", it) })
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Checkbox(checked = item.isTakenEvening, onCheckedChange = { onToggle("EVENING", it) })
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Checkbox(checked = item.isTakenBedtime, onCheckedChange = { onToggle("BEDTIME", it) })
        }
        
        TableCell(text = "${item.countThisMonth} lần", weight = 1.2f)
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(4.dp),
        style = if (isHeader) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) 
                else MaterialTheme.typography.bodyMedium,
        fontSize = if (isHeader) 13.sp else 12.sp
    )
}
