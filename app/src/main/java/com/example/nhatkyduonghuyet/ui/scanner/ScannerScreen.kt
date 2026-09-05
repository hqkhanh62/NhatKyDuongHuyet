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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.domain.GlucosePolicy
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.ScanSource
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.example.nhatkyduonghuyet.ui.camera.GlucoseCameraScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DANGER_THRESHOLD_MMOL = 13.0f

/**
 * Full-screen scanner.
 *
 * A scanned number is **never** written to the diary on its own: it is proposed in a
 * confirmation card where the user can correct it, choose before/after meal, save or rescan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    navController: NavController,
    scanner: GlucoseScanner,
    onGlucoseDetected: (ScannedGlucoseResult, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pending by remember { mutableStateOf<ScannedGlucoseResult?>(null) }
    var editedValue by remember { mutableStateOf("") }
    var afterMeal by remember { mutableStateOf(false) }
    var scanSession by remember { mutableStateOf(0) }
    var showRedFlash by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    suspend fun triggerDangerAlert() {
        repeat(2) {
            showRedFlash = true
            cameraControl?.enableTorch(true)
            delay(150)
            cameraControl?.enableTorch(false)
            showRedFlash = false
            delay(150)
        }
    }

    fun vibrate(isDangerous: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
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
            vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(120)
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
            // scanSession forces a fresh scanner (and a fresh vote window) on "Quét lại".
            androidx.compose.runtime.key(scanSession) {
                GlucoseCameraScanner(
                    scanner = scanner,
                    modifier = Modifier.fillMaxSize(),
                    onResult = { result ->
                        vibrate(false)
                        editedValue = "%.1f".format(result.value)
                        pending = result
                    },
                    onCameraBound = { camera -> cameraControl = camera.cameraControl },
                    overlayContent = {
                        if (showRedFlash) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red.copy(alpha = 0.6f))
                            )
                        }
                    }
                )
            }

            pending?.let { result ->
                val parsed = editedValue.replace(',', '.').toFloatOrNull()
                val isValid = parsed != null && GlucosePolicy.isValid(parsed)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF10151B),
                    contentColor = Color.White
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Xác nhận chỉ số vừa quét",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Hãy đối chiếu với màn hình máy đo trước khi lưu." +
                                sourceSuffix(result.source),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = editedValue,
                                onValueChange = { editedValue = it },
                                singleLine = true,
                                isError = !isValid,
                                label = { Text("mmol/L") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(150.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                FilterChip(
                                    selected = !afterMeal,
                                    onClick = { afterMeal = false },
                                    label = { Text("Trước ăn") }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FilterChip(
                                    selected = afterMeal,
                                    onClick = { afterMeal = true },
                                    label = { Text("Sau ăn") }
                                )
                            }
                        }
                        if (!isValid) {
                            Text(
                                "Chỉ số phải nằm trong khoảng " +
                                    "${GlucosePolicy.MIN_GLUCOSE_MMOL}–${GlucosePolicy.MAX_GLUCOSE_MMOL} mmol/L",
                                color = Color(0xFFFF8A80),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                enabled = isValid,
                                onClick = {
                                    val value = parsed ?: return@Button
                                    val isDanger = value > DANGER_THRESHOLD_MMOL
                                    vibrate(isDanger)
                                    if (isDanger) scope.launch { triggerDangerAlert() }
                                    onGlucoseDetected(result.copy(value = value), afterMeal)
                                    pending = null
                                    navController.popBackStack()
                                }
                            ) { Text("Lưu vào nhật ký") }
                            OutlinedButton(
                                onClick = {
                                    pending = null
                                    scanSession++
                                }
                            ) { Text("Quét lại") }
                        }
                    }
                }
            }
        }
    }
}

private fun sourceSuffix(source: ScanSource): String = when (source) {
    ScanSource.CONSENSUS -> " (hai bộ đọc cho cùng kết quả)"
    ScanSource.SEVEN_SEGMENT -> " (đọc từ các đoạn LED)"
    ScanSource.ML_KIT -> " (đọc bằng nhận dạng chữ)"
}
