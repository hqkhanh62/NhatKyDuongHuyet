package com.example.nhatkyduonghuyet.ui.scanner

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    navController: androidx.navigation.NavController,
    scanner: GlucoseScanner,
    onGlucoseDetected: (ScannedGlucoseResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(cameraExecutor) {
        onDispose { cameraExecutor.shutdown() }
    }
    val scope = rememberCoroutineScope()
    
    var lastResult by remember { mutableStateOf<ScannedGlucoseResult?>(null) }
    var showRedFlash by remember { mutableStateOf(false) }
    var cameraControlState by remember { mutableStateOf<CameraControl?>(null) }
    val isProcessing = remember { AtomicBoolean(false) }
    val hasResult = remember { AtomicBoolean(false) }

    val permissionState = remember { mutableStateOf(false) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionState.value = isGranted
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    suspend fun triggerDangerAlert() {
        // Double pulse flash effect
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
            val pattern = longArrayOf(0, dot, gap, dot, gap, dot, letterGap, dash, gap, dash, gap, dash, letterGap, dot, gap, dot, gap, dot)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(text = "Quét máy đo Glucose", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (permissionState.value) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null && !hasResult.get() && isProcessing.compareAndSet(false, true)) {
                                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                            scanner.processImage(
                                                image,
                                                onSuccess = { result ->
                                                    hasResult.set(true)
                                                    val isDanger = result.value > 13.0f
                                                    triggerHealthVibration(isDanger)
                                                    if (isDanger) {
                                                        scope.launch { triggerDangerAlert() }
                                                    }
                                                    
                                                    lastResult = result
                                                    onGlucoseDetected(result)
                                                },
                                                onNoResult = {},
                                                onError = { Log.w("Scanner", "OCR failed", it) },
                                                onComplete = {
                                                    isProcessing.set(false)
                                                    imageProxy.close()
                                                }
                                            )
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                                cameraControlState = camera.cameraControl
                            } catch (e: Exception) {
                                Log.e("Scanner", "Binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // The Overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Căn giữa màn hình máy đo",
                        modifier = Modifier.align(Alignment.Center).padding(top = 220.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Red Flash Overlay UI
                if (showRedFlash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Red.copy(alpha = 0.6f))
                    )
                }

                lastResult?.let { result ->
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (result.value > 13.0f) Color(0xFFB71C1C) else Color.Black.copy(alpha = 0.85f),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(if (result.value > 13.0f) "CẢNH BÁO NGUY HIỂM" else "Phát hiện chỉ số", style = MaterialTheme.typography.labelSmall)
                                Text("${result.value} mmol/L", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { navController.popBackStack() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cần cấp quyền Camera")
                }
            }
        }
    }
}
