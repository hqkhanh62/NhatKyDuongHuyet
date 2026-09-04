package com.example.nhatkyduonghuyet.ui.backup

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.data.backup.BackupPart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One screen for everything related to keeping data safe.
 *
 * Previously export/import lived in two different overflow menus (diary in
 * DateListScreen, medication in MedicationScreen) and there was no single place
 * telling the user whether their data was actually recoverable. Because the app
 * deliberately keeps `allowBackup="false"`, that visibility matters more than
 * the buttons themselves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val lastBackupAt by viewModel.lastBackupAt.collectAsState()
    val daysSinceExport by viewModel.daysSinceExport.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingExport by remember { mutableStateOf<BackupPart?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val part = pendingExport
        pendingExport = null
        if (uri != null && part != null) viewModel.export(part, uri)
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restore(it, context.displayNameOf(it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sao lưu & Khôi phục") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (state.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            // The single most important thing to tell the user, given that
            // uninstalling the app deletes everything.
            ExportReminderCard(daysSinceExport)

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Dữ liệu hiện có", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    DataRow(BackupPart.DIARY.label, state.diaryCount)
                    DataRow(BackupPart.PRESCRIPTION.label, state.medicationCount)
                    DataRow(BackupPart.MEDICATION_HISTORY.label, state.medicationLogCount)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Sao lưu tự động trong máy: ${lastBackupAt.asRelativeTime()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Bản sao lưu này mất khi gỡ app. Hãy xuất file để giữ lâu dài.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Xuất ra file", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            BackupPart.entries.forEach { part ->
                OutlinedButton(
                    onClick = {
                        pendingExport = part
                        exportLauncher.launch(viewModel.fileNameFor(part))
                    },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.height(0.dp))
                    Text("  ${part.label}")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Khôi phục", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("text/*", "text/csv", "*/*")) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Text("  Chọn file CSV để khôi phục")
            }
            OutlinedButton(
                onClick = { viewModel.restoreFromRollingSnapshot() },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Text("  Khôi phục từ bản sao lưu trong máy")
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Khôi phục chỉ thêm và cập nhật, không xoá dữ liệu đang có. " +
                    "Nhập lại cùng một file nhiều lần cũng không tạo bản trùng.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { viewModel.backupNow() },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Backup, contentDescription = null)
                Text("  Sao lưu ngay (trong máy)")
            }
        }
    }
}

@Composable
private fun ExportReminderCard(daysSinceExport: Long?) {
    val overdue = daysSinceExport == null || daysSinceExport >= EXPORT_REMINDER_DAYS
    if (!overdue) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = "Đã xuất file cách đây $daysSinceExport ngày. Dữ liệu của bạn an toàn.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(0.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    text = if (daysSinceExport == null) "Bạn chưa từng xuất file sao lưu"
                    else "Đã $daysSinceExport ngày chưa xuất file sao lưu",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "App không sao lưu lên đám mây. Nếu gỡ app hoặc mất máy, " +
                        "dữ liệu sẽ mất vĩnh viễn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun DataRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("$count dòng", style = MaterialTheme.typography.bodyMedium)
    }
}

private const val EXPORT_REMINDER_DAYS = 30L

private fun Long.asRelativeTime(): String =
    if (this <= 0L) "chưa có"
    else SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(this))

private fun android.content.Context.displayNameOf(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
    }
}.getOrNull()
