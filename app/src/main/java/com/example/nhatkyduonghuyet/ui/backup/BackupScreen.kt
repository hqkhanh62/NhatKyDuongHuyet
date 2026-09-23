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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
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
import com.example.nhatkyduonghuyet.data.backup.BackupBundle
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
    var showIndividual by remember { mutableStateOf(false) }

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

    val bundleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupBundle.MIME_TYPE)
    ) { uri: Uri? -> uri?.let { viewModel.exportBundle(it) } }

    // Persistent folder grant for the weekly unattended export.
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> uri?.let { viewModel.enableAutoExport(it) } }

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

            // Leading action: one tap, one file, everything in it. Exporting
            // the three files separately invites a half-finished backup.
            Button(
                onClick = { bundleLauncher.launch(viewModel.bundleFileName()) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Archive, contentDescription = null)
                Text("  Xuất toàn bộ (1 tệp)")
            }
            Text(
                "Gồm cả 3 loại dữ liệu trong một tệp .zip duy nhất. Nên dùng cách này.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showIndividual = !showIndividual }) {
                Text(if (showIndividual) "Ẩn xuất từng phần" else "Xuất từng phần riêng lẻ")
            }
            if (showIndividual) {
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
                        Text("  ${part.label}")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            AutoExportCard(
                enabled = state.autoExportEnabled,
                folderName = state.autoExportFolderName,
                onPickFolder = { folderLauncher.launch(null) },
                onDisable = { viewModel.disableAutoExport() }
            )

            Spacer(Modifier.height(16.dp))

            Text("Khôi phục", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/zip", "text/*", "*/*")) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Text("  Chọn tệp sao lưu để khôi phục")
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

/**
 * Weekly automatic export.
 *
 * Deliberately framed as "choose a folder" rather than a bare on/off switch:
 * a background job cannot write anywhere without a persisted SAF grant, so the
 * folder *is* the setting. Wording nudges towards a synced folder, because an
 * automatic export that lands only on the same phone still dies with the phone.
 */
@Composable
private fun AutoExportCard(
    enabled: Boolean,
    folderName: String?,
    onPickFolder: () -> Unit,
    onDisable: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Tự động xuất hằng tuần", fontWeight = FontWeight.Bold)
                    Text(
                        text = if (enabled) {
                            "Đang bật. Thư mục: ${folderName ?: "đã chọn"}"
                        } else {
                            "Đang tắt"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (checked) onPickFolder() else onDisable()
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Mỗi tuần app sẽ tự lưu một tệp sao lưu đầy đủ vào thư mục bạn " +
                    "chọn. Nếu tuần đó bạn đã tự xuất file thì app sẽ bỏ qua.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Nên chọn thư mục có đồng bộ đám mây (ví dụ Google Drive). " +
                    "Thư mục nằm trong máy sẽ mất cùng máy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (enabled) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onPickFolder) {
                    Text("Đổi thư mục")
                }
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
