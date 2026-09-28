package com.example.nhatkyduonghuyet.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.ml.GlucoseScanner

/**
 * Màn hình quét máy đo bằng camera AI.
 *
 * Luồng "Pro AI": GlucoseCameraPreview phân tích frame -> [ScanViewModel] làm
 * sạch (Auto Clean 2.0-30.0), khoá chỉ số ổn định, tự điền ngày/giờ/buổi
 * (Auto Import) -> banner kết quả ngay dưới camera để kiểm tra -> xác nhận là
 * dữ liệu vào thẳng Room và vòng dự báo AI (LSTM + HbA1c + cảnh báo) chạy lại.
 */
@Composable
fun ScannerScreen(
    navController: NavController,
    scanner: GlucoseScanner,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var torchOn by remember { mutableStateOf(false) }
    var showRedFlash by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Rung + chớp đỏ theo mức rủi ro mà ScanInsightEngine vừa tính.
    val isCritical = when (val phase = state.phase) {
        is ScanPhase.Review -> phase.insight?.isCritical == true
        is ScanPhase.Saved -> phase.insight.isCritical
        ScanPhase.Scanning -> false
    }
    LaunchedEffect(state.phase) {
        when (val phase = state.phase) {
            is ScanPhase.Review -> triggerHealthVibration(context, phase.insight?.isCritical == true)
            is ScanPhase.Saved -> triggerHealthVibration(context, phase.insight.isCritical)
            ScanPhase.Scanning -> Unit
        }
    }
    LaunchedEffect(isCritical) {
        if (isCritical) {
            repeat(3) {
                showRedFlash = true
                runCatching { cameraControl?.enableTorch(true) }
                kotlinx.coroutines.delay(150)
                showRedFlash = false
                runCatching { cameraControl?.enableTorch(false) }
                kotlinx.coroutines.delay(150)
            }
            kotlinx.coroutines.delay(400)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (permissionGranted) {
            GlucoseCameraPreview(
                scanner = scanner,
                modifier = Modifier.fillMaxSize(),
                enabled = state.shouldAnalyze,
                torchEnabled = torchOn,
                onCameraReady = { control -> cameraControl = control },
                onError = { error ->
                    Log.e("Scanner", "Camera error", error)
                    cameraError = error.localizedMessage ?: "Không mở được camera"
                },
                onOcrFields = viewModel::onOcrFields,
                overlay = { spec -> ScanAlignmentOverlay(spec, state.overlay) },
                onResult = { result -> viewModel.onFrame(result) }
            )

            ScannerTopBar(
                autoSave = state.autoSave,
                torchOn = torchOn,
                onBack = { navController.popBackStack() },
                onToggleTorch = { torchOn = !torchOn },
                onToggleAutoSave = { viewModel.setAutoSave(it) }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                cameraError?.let {
                    Text(
                        text = "Lỗi camera: $it",
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                when (val phase = state.phase) {
                    is ScanPhase.Review -> ScanReviewBanner(
                        draft = phase.draft,
                        insight = phase.insight,
                        forecast = phase.forecast,
                        isSaving = phase.isSaving || state.isBusy,
                        onConfirm = viewModel::confirm,
                        onRescan = viewModel::scanAnother,
                        onAdjustValue = viewModel::adjustValue,
                        onSelectSession = viewModel::selectSession,
                        onSelectSlot = viewModel::selectSlot,
                        onUseSystemTime = viewModel::useSystemTime,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    is ScanPhase.Saved -> ScanSavedBanner(
                        saved = phase,
                        retrainRequested = state.retrainRequested,
                        onUndo = viewModel::undo,
                        onScanAgain = {
                            viewModel.consumeRetrainNotice()
                            viewModel.scanAnother()
                        },
                        onDone = { navController.popBackStack() },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    ScanPhase.Scanning -> ScanningHint(
                        statusMessage = state.statusMessage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            }

            if (showRedFlash) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.5f)))
            }
        } else {
            PermissionNeeded(
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun ScannerTopBar(
    autoSave: Boolean,
    torchOn: Boolean,
    onBack: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleAutoSave: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        color = Color.Black.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Quét máy đo - AI OCR",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "ML Kit • nhận diện số, giờ và ngày trên màn hình",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onToggleTorch) {
                Icon(
                    if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Bật/tắt đèn",
                    tint = if (torchOn) Color.Yellow else Color.White
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 6.dp)) {
                Switch(checked = autoSave, onCheckedChange = onToggleAutoSave)
                Text("Tự lưu", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ScanningHint(statusMessage: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Đưa màn hình máy đo lấp đầy khung và giữ yên",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            "Cách 15-20cm, tránh bóng loá trên mặt kính • chạm vào màn hình để lấy nét",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
        statusMessage?.let {
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = it,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionNeeded(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.FlashOff,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(40.dp)
        )
        Text("Cần cấp quyền Camera để quét máy đo.", color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            "Ảnh chỉ được xử lý trên máy, không gửi đi đâu.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        androidx.compose.material3.Button(onClick = onRequest) {
            Text("Cấp quyền camera")
        }
    }
}

/** Chuỗi rung SOS cho cảnh báo nguy hiểm, rung ngắn cho chỉ số bình thường. */
private fun triggerHealthVibration(context: Context, isDangerous: Boolean) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (isDangerous) {
        val dot = 150L
        val dash = 450L
        val gap = 100L
        val letterGap = 300L
        val pattern = longArrayOf(
            0, dot, gap, dot, gap, dot, letterGap,
            dash, gap, dash, gap, dash, letterGap,
            dot, gap, dot, gap, dot
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(200)
    }
}
