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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScannerScreen(
    navController: NavController,
    scanner: GlucoseScanner,
    onGlucoseDetected: (ScannedGlucoseResult) -> Unit
) {
    val context = LocalContext.current
    val hasDetectedSuccess = remember { AtomicBoolean(false) }
    val scope = rememberCoroutineScope()

    var lastResult by remember { mutableStateOf<ScannedGlucoseResult?>(null) }
    var showRedFlash by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    val recentValues = remember { java.util.ArrayDeque<Float>() }
    var cameraControlState by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
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

    suspend fun triggerDangerAlert() {
        showRedFlash = true
        cameraControlState?.enableTorch(true)
        delay(150)
        cameraControlState?.enableTorch(false)
        showRedFlash = false
        delay(150)
        showRedFlash = true
        cameraControlState?.enableTorch(true)
        delay(150)
        cameraControlState?.enableTorch(false)
        delay(500)
        showRedFlash = false
    }

    fun triggerHealthVibration(isDangerous: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

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

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                    Text(
                        text = "Quét máy đo Glucose",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (permissionGranted) {
                GlucoseCameraPreview(
                    scanner = scanner,
                    modifier = Modifier.fillMaxSize(),
                    enabled = !hasDetectedSuccess.get(),
                    torchEnabled = torchOn,
                    onCameraReady = { cameraControlState = it },
                    onError = { Log.e("Scanner", "Camera error", it) },
                    onResult = { result ->
                        val stable = synchronized(recentValues) {
                            recentValues.addLast(result.value)
                            while (recentValues.size > STABILITY_WINDOW_SIZE) {
                                recentValues.removeFirst()
                            }
                            findStableValue(recentValues)
                        }
                        if (stable != null && hasDetectedSuccess.compareAndSet(false, true)) {
                            val stableResult = result.copy(value = stable)
                            val isDanger = stableResult.value > 13.0f
                            triggerHealthVibration(isDanger)
                            if (isDanger) {
                                scope.launch { triggerDangerAlert() }
                            }
                            lastResult = stableResult
                            onGlucoseDetected(stableResult)
                        }
                    }
                )

                IconButton(
                    onClick = { torchOn = !torchOn },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                ) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = "Bật/tắt đèn",
                        tint = if (torchOn) Color.Yellow else Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Đưa màn hình máy đo lấp đầy khung xanh và giữ yên",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Giữ cách 15-20cm, tránh bóng loá trên mặt kính",
                        color = Color.White
                    )
                }

                if (showRedFlash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Red.copy(alpha = 0.6f))
                    )
                }

                lastResult?.let { result ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (result.value > 13.0f) Color(0xFFB71C1C) else Color.Black.copy(alpha = 0.85f),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    if (result.value > 13.0f) "CẢNH BÁO NGUY HIỂM" else "Phát hiện chỉ số",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "${result.value} mmol/L",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(onClick = { navController.popBackStack() }) {
                                Text("OK")
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cần cấp quyền Camera để quét máy đo.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Cấp quyền camera")
                    }
                }
            }
        }
    }
}

private const val STABILITY_WINDOW_SIZE = 4
private const val STABILITY_REQUIRED_MATCHES = 3
private const val STABILITY_TOLERANCE = 0.15f

private fun findStableValue(values: java.util.ArrayDeque<Float>): Float? {
    if (values.size < STABILITY_REQUIRED_MATCHES) return null
    val latest = values.peekLast()
    val matches = values.count { kotlin.math.abs(it - latest) <= STABILITY_TOLERANCE }
    return if (matches >= STABILITY_REQUIRED_MATCHES) latest else null
}
