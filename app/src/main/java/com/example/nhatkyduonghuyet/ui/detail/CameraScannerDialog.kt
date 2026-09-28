package com.example.nhatkyduonghuyet.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.nhatkyduonghuyet.domain.scanner.AutoImportPipeline
import com.example.nhatkyduonghuyet.domain.scanner.FieldSource
import com.example.nhatkyduonghuyet.domain.scanner.GlucoseSession
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.MeterDisplayFields
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.example.nhatkyduonghuyet.ml.StableReadingTracker
import com.example.nhatkyduonghuyet.ui.scanner.GlucoseCameraPreview
import com.example.nhatkyduonghuyet.ui.scanner.ScanAlignmentOverlay
import com.example.nhatkyduonghuyet.ui.scanner.ScanOverlayState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Scanning dialog opened from DayDetail.
 *
 * Nó dùng lại [GlucoseCameraPreview] (cùng ROI + overlay với màn hình quét toàn
 * màn hình) và thêm bước kiểm tra: sau khi AI khoá được chỉ số, banner hiện
 * ngay dưới camera cho người dùng xác nhận *trước khi* số liệu được điền vào ô
 * đang nhập. Giờ lấy trên màn hình máy đo được ưu tiên, nếu máy không hiển thị
 * giờ thì dùng giờ điện thoại.
 */
@Composable
fun CameraScannerDialog(
    scanner: GlucoseScanner,
    onDismiss: () -> Unit,
    onResult: (ScannedGlucoseResult) -> Unit
) {
    val context = LocalContext.current
    val hasDeliveredResult = remember { AtomicBoolean(false) }
    val stableTracker = remember { StableReadingTracker() }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingResult by remember { mutableStateOf<ScannedGlucoseResult?>(null) }
    var liveFields by remember { mutableStateOf<MeterDisplayFields?>(null) }
    var hits by remember { mutableStateOf(0) }
    var torchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var statusText by remember { mutableStateOf("Đưa màn hình máy đo vào khung và giữ yên…") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) statusText = "Cần cấp quyền camera để quét máy đo."
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted && pendingResult == null) {
            kotlinx.coroutines.delay(SCAN_FEEDBACK_TIMEOUT_MS)
            if (pendingResult == null) {
                statusText = "Chưa đọc được. Giữ máy đo cách 15-20cm, tránh lóa, " +
                    "hoặc bật đèn flash rồi thử lại."
            }
        }
    }

    val systemTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val resolved = pendingResult?.let { result ->
        val meterTime = result.fields.time?.formatted
        val time = meterTime ?: systemTime
        QuickReviewInfo(
            valueText = AutoImportPipeline.formatMmol(result.value),
            time = time,
            timeSource = if (meterTime != null) FieldSource.METER else FieldSource.SYSTEM,
            sessionLabel = GlucoseSession.fromTime(time)?.label ?: "-",
            source = result.source,
            confidence = result.confidence,
            hasDate = result.date != null,
            dateText = result.fields.date?.dayMonth
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.96f),
        title = { Text("Quét máy đo - AI OCR") },
        confirmButton = {},
        dismissButton = {
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("HỦY") }
            }
        },
        text = {
            if (!permissionGranted) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(statusText)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Cấp quyền camera")
                    }
                }
            } else {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 360.dp, max = 460.dp)
                    ) {
                        val review = pendingResult
                        GlucoseCameraPreview(
                            scanner = scanner,
                            modifier = Modifier.fillMaxWidth().height(420.dp),
                            enabled = review == null,
                            torchEnabled = torchOn,
                            onCameraReady = { cameraControl = it },
                            onError = { error ->
                                statusText = "Không thể mở camera: " +
                                    (error.localizedMessage ?: "lỗi không xác định")
                            },
                            onOcrFields = { fields -> liveFields = fields },
                            overlay = { spec ->
                                ScanAlignmentOverlay(
                                    spec = spec,
                                    state = ScanOverlayState(
                                        locked = review != null,
                                        hits = hits,
                                        required = stableTracker.requiredMatches,
                                        liveValue = review?.value?.let { AutoImportPipeline.formatMmol(it) },
                                        meterTime = liveFields?.time?.formatted,
                                        meterDate = liveFields?.date?.dayMonth,
                                        hint = if (review == null) statusText else null
                                    )
                                )
                            },
                            onResult = { result ->
                                if (hasDeliveredResult.get()) return@GlucoseCameraPreview
                                // Auto Clean trước khi bỏ phiếu: giá trị ngoài
                                // ngưỡng 2.0-30.0 hoặc mã lỗi máy đo không được
                                // xuất hiện ở banner kiểm tra.
                                val cleaned = AutoImportPipeline.clean(
                                    raw = result.value,
                                    meterErrorCode = liveFields?.errorCode
                                )
                                when (cleaned) {
                                    is AutoImportPipeline.CleanResult.Accepted -> {
                                        val stableValue = stableTracker.offer(cleaned.value)
                                        if (stableValue != null) {
                                            hasDeliveredResult.set(true)
                                            runCatching { cameraControl?.enableTorch(false) }
                                            pendingResult = result.copy(value = stableValue)
                                        } else {
                                            hits += 1
                                        }
                                    }

                                    is AutoImportPipeline.CleanResult.Rejected -> {
                                        statusText = when (cleaned.reason) {
                                            AutoImportPipeline.RejectReason.OUT_OF_RANGE ->
                                                "Ngoài ngưỡng an toàn 2.0-30.0 (${cleaned.detail ?: "-"} mmol/L)"
                                            AutoImportPipeline.RejectReason.METER_ERROR ->
                                                "Máy đo đang báo lỗi ${cleaned.detail ?: ""}"
                                            else -> "Chưa đọc được chỉ số - giữ yên màn hình"
                                        }
                                    }
                                }
                            }
                        )

                        IconButton(
                            onClick = { torchOn = !torchOn },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FlashOn,
                                contentDescription = "Bật/tắt đèn",
                                tint = if (torchOn) Color.Yellow else Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val info = resolved
                    if (info != null) {
                        ScanQuickReviewBanner(
                            info = info,
                            onConfirm = {
                                pendingResult?.let { onResult(it) }
                            },
                            onRescan = {
                                stableTracker.clear()
                                hasDeliveredResult.set(false)
                                hits = 0
                                pendingResult = null
                                statusText = "Đưa màn hình máy đo vào khung và giữ yên…"
                            }
                        )
                    } else {
                        Text(statusText, fontSize = 12.sp)
                    }
                }
            }
        }
    )
}

private data class QuickReviewInfo(
    val valueText: String,
    val time: String,
    val timeSource: FieldSource,
    val sessionLabel: String,
    val source: String,
    val confidence: Float,
    val hasDate: Boolean,
    val dateText: String?
)

/** Banner xác nhận nhanh trong đối thoại: chỉ số + giờ/ngày AI tự điền. */
@Composable
private fun ScanQuickReviewBanner(
    info: QuickReviewInfo,
    onConfirm: () -> Unit,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = info.valueText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(" mmol/L", fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text(
                text = info.source + (if (info.confidence >= 0.6f) " • tin cậy cao" else " • nên kiểm tra lại"),
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Giờ ${info.time} (${info.timeSource.label})", fontSize = 12.sp)
            Text("Buổi ${info.sessionLabel}", fontSize = 12.sp)
            Text(
                text = if (info.hasDate) "Ngày ${info.dateText} (máy đo)" else "Máy đo không có ngày",
                fontSize = 12.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                Text("ĐIỀN VÀO Ô", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onRescan) {
                Text("QUÉT LẠI", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private const val SCAN_FEEDBACK_TIMEOUT_MS = 8_000L
