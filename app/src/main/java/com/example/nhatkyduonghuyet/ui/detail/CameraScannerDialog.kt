package com.example.nhatkyduonghuyet.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.example.nhatkyduonghuyet.ui.scanner.GlucoseCameraPreview
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Scanning dialog opened from DayDetail.
 *
 * It now reuses [GlucoseCameraPreview], so it analyses exactly the region the
 * user frames in green - identical behaviour to the full-screen scanner opened
 * from the dashboard. The preview is also given a much larger surface, because
 * a tiny preview forces the user to hold the meter far away and shrinks the
 * digits below what OCR can read reliably.
 */
@Composable
fun CameraScannerDialog(
    scanner: GlucoseScanner,
    onDismiss: () -> Unit,
    onResult: (ScannedGlucoseResult) -> Unit
) {
    val context = LocalContext.current
    val hasDeliveredResult = remember { AtomicBoolean(false) }
    val recentValues = remember { ArrayDeque<Float>() }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var resultDelivered by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var statusText by remember { mutableStateOf("Đưa màn hình máy đo vào khung xanh…") }

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
        if (permissionGranted) {
            kotlinx.coroutines.delay(SCAN_FEEDBACK_TIMEOUT_MS)
            if (!hasDeliveredResult.get()) {
                statusText = "Chưa đọc được. Giữ máy đo cách 15-20cm, tránh lóa, " +
                    "hoặc bật đèn flash rồi thử lại."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.96f),
        title = { Text("Quét máy đo") },
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
                        GlucoseCameraPreview(
                            scanner = scanner,
                            modifier = Modifier.fillMaxWidth().height(420.dp),
                            enabled = !resultDelivered,
                            torchEnabled = torchOn,
                            onCameraReady = { cameraControl = it },
                            onError = { error ->
                                statusText = "Không thể mở camera: " +
                                    (error.localizedMessage ?: "lỗi không xác định")
                            },
                            onResult = { result ->
                                if (hasDeliveredResult.get()) return@GlucoseCameraPreview
                                val stableValue = synchronized(recentValues) {
                                    recentValues.addLast(result.value)
                                    while (recentValues.size > STABILITY_WINDOW_SIZE) {
                                        recentValues.removeFirst()
                                    }
                                    findStableValue(recentValues)
                                }
                                if (stableValue != null &&
                                    hasDeliveredResult.compareAndSet(false, true)
                                ) {
                                    resultDelivered = true
                                    onResult(result.copy(value = stableValue))
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
                    Text(statusText)
                }
            }
        }
    )

    LaunchedEffect(resultDelivered) {
        if (resultDelivered) onDismiss()
    }
}

private const val SCAN_FEEDBACK_TIMEOUT_MS = 8_000L
private const val STABILITY_WINDOW_SIZE = 4
private const val STABILITY_REQUIRED_MATCHES = 3
private const val STABILITY_TOLERANCE = 0.15f

private fun findStableValue(values: ArrayDeque<Float>): Float? {
    if (values.size < STABILITY_REQUIRED_MATCHES) return null
    val latest = values.peekLast()
    val matches = values.count { kotlin.math.abs(it - latest) <= STABILITY_TOLERANCE }
    return if (matches >= STABILITY_REQUIRED_MATCHES) latest else null
}
