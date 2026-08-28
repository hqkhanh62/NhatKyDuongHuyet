package com.example.nhatkyduonghuyet.ui.detail

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.google.mlkit.vision.common.InputImage
import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun CameraScannerDialog(
    scanner: GlucoseScanner,
    onDismiss: () -> Unit,
    onResult: (ScannedGlucoseResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isProcessing = remember { AtomicBoolean(false) }
    val hasDeliveredResult = remember { AtomicBoolean(false) }
    val lastAttemptAt = remember { AtomicLong(0L) }
    // Keep this window scoped to the current dialog. A new scan must not reuse
    // a value obtained by a previous camera session.
    val recentValues = remember { ArrayDeque<Float>() }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var resultDelivered by remember { mutableStateOf(false) }
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
                statusText = "Chưa đọc được. Giữ máy đo yên, tránh lóa rồi thử lại."
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdownNow()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("HỦY") }
        },
        text = {
            if (!permissionGranted) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(statusText)
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Cấp quyền camera")
                    }
                }
            } else {
                Box(modifier = Modifier.size(320.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val provider = cameraProviderFuture.get()
                                cameraProvider = provider

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
                                                !hasDeliveredResult.get() &&
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

                                            val inputImage = InputImage.fromMediaImage(
                                                mediaImage!!,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            scanner.processImage(
                                                inputImage,
                                                onResult = { result ->
                                                    isProcessing.set(false)
                                                    imageProxy.close()
                                                    if (result != null && !hasDeliveredResult.get()) {
                                                        val stableValue = synchronized(recentValues) {
                                                            recentValues.addLast(result.value)
                                                            while (recentValues.size > STABILITY_WINDOW_SIZE) {
                                                                recentValues.removeFirst()
                                                            }
                                                            findStableValue(recentValues)
                                                        }

                                                        if (stableValue != null &&
                                                            hasDeliveredResult.compareAndSet(false, true)) {
                                                            resultDelivered = true
                                                            onResult(result.copy(value = stableValue))
                                                        }
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
                                    val focusFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                                    val focusPoint = focusFactory.createPoint(0.5f, 0.5f)
                                    camera.cameraControl.startFocusAndMetering(
                                        FocusMeteringAction.Builder(focusPoint)
                                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                            .build()
                                    )
                                } catch (error: Exception) {
                                    statusText = "Không thể mở camera: ${error.localizedMessage ?: "lỗi không xác định"}"
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        modifier = Modifier
                            .size(width = 240.dp, height = 150.dp)
                            .align(Alignment.Center),
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, Color.Green)
                    ) {}

                    Text(
                        text = statusText,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        color = Color.White
                    )
                }
            }
        }
    )

    LaunchedEffect(resultDelivered) {
        if (resultDelivered) onDismiss()
    }
}

private const val ANALYSIS_INTERVAL_MS = 250L
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
