package com.example.nhatkyduonghuyet.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun ScannerScreen(
    navController: NavController,
    scanner: GlucoseScanner,
    onGlucoseDetected: (ScannedGlucoseResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isProcessing = remember { AtomicBoolean(false) }
    val hasDetectedSuccess = remember { AtomicBoolean(false) }
    val lastAttemptAt = remember { AtomicLong(0L) }
    val cameraProviderRef = remember { AtomicReference<ProcessCameraProvider?>(null) }
    val scope = rememberCoroutineScope()

    var lastResult by remember { mutableStateOf<ScannedGlucoseResult?>(null) }
    var showRedFlash by remember { mutableStateOf(false) }
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

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef.get()?.unbindAll()
            cameraExecutor.shutdownNow()
        }
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
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            cameraProviderRef.set(provider)
                            val preview = Preview.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .build()
                                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val now = System.currentTimeMillis()
                                        val mediaImage = imageProxy.image
                                        val shouldAnalyze = mediaImage != null &&
                                            !hasDetectedSuccess.get() &&
                                            !isProcessing.get() &&
                                            now - lastAttemptAt.get() >= ANALYSIS_INTERVAL_MS

                                        if (!shouldAnalyze) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        lastAttemptAt.set(now)
                                        if (!isProcessing.compareAndSet(false, true)) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        scanner.processImage(
                                            InputImage.fromMediaImage(
                                                mediaImage!!,
                                                imageProxy.imageInfo.rotationDegrees
                                            ),
                                            onResult = { result ->
                                                isProcessing.set(false)
                                                imageProxy.close()
                                                if (result != null && hasDetectedSuccess.compareAndSet(false, true)) {
                                                    val isDanger = result.value > 13.0f
                                                    triggerHealthVibration(isDanger)
                                                    if (isDanger) {
                                                        scope.launch {
                                                            triggerDangerAlert()
                                                        }
                                                    }
                                                    lastResult = result
                                                    onGlucoseDetected(result)
                                                }
                                            },
                                            onError = {
                                                isProcessing.set(false)
                                                imageProxy.close()
                                            }
                                        )
                                    }
                                }

                            try {
                                provider.unbindAll()
                                val camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControlState = camera.cameraControl
                                val focusFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                                camera.cameraControl.startFocusAndMetering(
                                    FocusMeteringAction.Builder(focusFactory.createPoint(0.5f, 0.5f))
                                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                        .build()
                                )
                            } catch (error: Exception) {
                                Log.e("Scanner", "Binding failed", error)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier
                            .size(width = 280.dp, height = 200.dp)
                            .align(Alignment.Center),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            Color.Green.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {}

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Đưa màn hình máy đo vào khung xanh và giữ yên",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Hệ thống tự quét trong vài giây",
                            color = Color.White
                        )
                    }
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

private const val ANALYSIS_INTERVAL_MS = 250L
