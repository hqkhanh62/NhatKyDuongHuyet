package com.example.nhatkyduonghuyet.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.preference.ThemePreference
import com.example.nhatkyduonghuyet.util.LocaleHelper
import com.example.nhatkyduonghuyet.util.PdfExportHelper
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: LogEntryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePreference = remember { ThemePreference(context) }
    val localeHelper = remember { LocaleHelper(context) }
    
    val isDarkMode by themePreference.isDarkMode.collectAsState(initial = false)
    val currentLang = localeHelper.getCurrentLocale()
    val allEntries by viewModel.allLogEntries.collectAsState()

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            scope.launch {
                PdfExportHelper.exportToPdf(context, it, allEntries)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Chế độ tối")
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                themePreference.setDarkMode(enabled)
                                (context as? Activity)?.recreate()
                            }
                        }
                    )
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ngôn ngữ")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = currentLang == "vi",
                            onClick = {
                                localeHelper.setLocale("vi")
                                (context as? Activity)?.recreate()
                            },
                            label = { Text("Tiếng Việt") }
                        )
                        FilterChip(
                            selected = currentLang == "en",
                            onClick = {
                                localeHelper.setLocale("en")
                                (context as? Activity)?.recreate()
                            },
                            label = { Text("English") }
                        )
                    }
                }
            }

            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Xuất PDF báo cáo")
                    }
                    Button(onClick = { pdfLauncher.launch("bao-cao-duong-huyet") }) {
                        Text("Xuất")
                    }
                }
            }
        }
    }
}
