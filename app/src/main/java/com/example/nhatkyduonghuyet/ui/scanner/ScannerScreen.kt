package com.example.nhatkyduonghuyet.ui.scanner

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.CameraControl
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.example.nhatkyduonghuyet.ui.camera.GlucoseCameraScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DANGER_THRESHOLD_MMOL = 13.0f

@Composable
fun ScannerScreen(
    navController: NavController,
    scanner: GlucoseScanner,
    onGlucoseDetected: (ScannedGlucoseResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var lastResult by remember { mutableStateOf<ScannedGlucoseResult?>(null) }
    var showRedFlash by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    suspend fun triggerDangerAlert() {
        showRedFlash = true
        cameraControl?.enableTorch(true)
        delay(150)
        cameraControl?.enableTorch(false)
        showRedFlash = false
        delay(150)
        showRedFlash = true
        cameraControl?.enableTorch(true)
        delay(150)
        cameraControl?.enableTorch(false)
        delay(500)
        showRedFlash = false
    }

    fun triggerHealthVibration(isDangerous: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
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
            GlucoseCameraScanner(
                scanner = scanner,
                modifier = Modifier.fillMaxSize(),
                onResult = { result ->
                    val isDanger = result.value > DANGER_THRESHOLD_MMOL
                    triggerHealthVibration(isDanger)
                    if (isDanger) {
                        scope.launch {
                            triggerDangerAlert()
                        }
                    }
                    lastResult = result
                    onGlucoseDetected(result)
                },
                onCameraBound = { camera ->
                    cameraControl = camera.cameraControl
                },
                overlayContent = {
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
                            color = if (result.value > DANGER_THRESHOLD_MMOL) {
                                Color(0xFFB71C1C)
                            } else {
                                Color.Black.copy(alpha = 0.85f)
                            },
                            contentColor = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        if (result.value > DANGER_THRESHOLD_MMOL) {
                                            "CẢNH BÁO NGUY HIỂM"
                                        } else {
                                            "Phát hiện chỉ số"
                                        },
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
                }
            )
        }
    }
}
