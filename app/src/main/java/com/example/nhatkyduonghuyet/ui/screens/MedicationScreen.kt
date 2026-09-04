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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val snackbarHostState = remember { SnackbarHostState() }
    val backupMessage by viewModel.backupMessage.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeBackupMessage()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { viewModel.importPrescription(it) } }
    )

    val exportPrescriptionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri -> uri?.let { viewModel.exportPrescription(it) } }
    )

    val exportHistoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri -> uri?.let { viewModel.exportHistory(it) } }
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
                    IconButton(onClick = {
                        exportPrescriptionLauncher.launch(viewModel.prescriptionFileName())
                    }) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.export_prescription_csv)
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_actions))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_prescription_csv)) },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                exportPrescriptionLauncher.launch(viewModel.prescriptionFileName())
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_history_csv)) },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                exportHistoryLauncher.launch(viewModel.historyFileName())
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.import_csv)) },
                            leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                importLauncher.launch("*/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.backup_now)) },
                            leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                viewModel.backupNow()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.restore_from_backup)) },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                viewModel.restoreFromBackup()
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
        TableCell(text = stringResource(R.string.medication_instruction), weight = 3f, isHeader = true)
        
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
        TableCell(text = item.medication.instruction, weight = 3f)
        
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
