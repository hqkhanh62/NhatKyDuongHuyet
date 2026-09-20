package com.example.nhatkyduonghuyet.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.ui.navigation.GlucoseScreen
import com.example.nhatkyduonghuyet.ui.scanner.components.ScanReviewBanner
import com.example.nhatkyduonghuyet.ui.scanner.components.ScanSavedBanner
import com.example.nhatkyduonghuyet.ui.scanner.components.ScannerOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Pro AI Camera screen (Scan → Review → Save → Forecast → Warn).
 *
 * - Live CameraX stream + ML Kit hybrid OCR at ~4 fps.
 * - Professional alignment overlay with laser scan-line.
 * - Instant result banner under the camera for pre-confirm review:
 *   value, auto session, meter/system date-time chips, risk level and
 *   provisional HbA1c.
 * - On confirm, [ScannerViewModel] auto-saves via the Auto Import Pipeline
 *   and refreshes the LSTM forecast immediately (Real-time AI Loop).
 */
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun ScannerScreen(
    navController: NavController,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isProcessing = remember { AtomicBoolean(false) }
    val lastAttemptAt = remember { AtomicLong(0L) }
    val cameraProviderRef = remember { AtomicReference<ProcessCameraProvider?>(null) }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var showRedFlash by remember { mutableStateOf(false) }
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

    // Real-time AI Loop feedback: vibrate + flash the moment a stable
    // reading locks, with an SOS pattern for danger values.
    LaunchedEffect(state.phase, state.draft) {
        val draft = state.draft
        if (state.phase == ScannerPhase.REVIEW && draft != null) {
            ScanFeedback.vibrateForResult(context, draft.risk.isDanger)
            if (draft.risk.isDanger) {
                scope.launch {
                    repeat(2) {
                        showRedFlash = true
                        cameraControl?.enableTorch(true)
                        delay(180)
                        cameraControl?.enableTorch(false)
                        showRedFlash = false
                        delay(180)
                    }
                    cameraControl?.enableTorch(torchOn)
                }
            }
        }
        if (state.phase == ScannerPhase.SAVED) {
            ScanFeedback.vibrateSaved(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!permissionGranted) {
            ScannerPermissionContent(
                onBack = { navController.popBackStack() },
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
            return@Box
        }

        // 1. Live camera stream.
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
                                val shouldAnalyze = !isProcessing.get() &&
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

                                val bitmap = try {
                                    imageProxy.toBitmap()
                                } catch (_: Exception) {
                                    null
                                }
                                if (bitmap == null) {
                                    isProcessing.set(false)
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                viewModel.scanner.processHybrid(
                                    bitmap,
                                    imageProxy.imageInfo.rotationDegrees,
                                    onResult = { result ->
                                        isProcessing.set(false)
                                        imageProxy.close()
                                        viewModel.onFrameResult(result)
                                    },
                                    onError = {
                                        isProcessing.set(false)
                                        imageProxy.close()
                                        viewModel.onFrameResult(null)
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
                        cameraControl = camera.cameraControl
                        val focusFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                        camera.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(focusFactory.createPoint(0.5f, 0.5f))
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                        )
                    } catch (error: Exception) {
                        Log.e("ScannerPro", "Camera binding failed", error)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Professional alignment overlay.
        ScannerOverlay(
            scanning = state.phase == ScannerPhase.SCANNING,
            isDanger = state.draft?.risk?.isDanger == true &&
                state.phase != ScannerPhase.SCANNING
        )

        // 3. Top bar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color.White
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Quét AI Pro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            IconButton(
                onClick = {
                    torchOn = !torchOn
                    cameraControl?.enableTorch(torchOn)
                }
            ) {
                Icon(
                    if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Đèn flash",
                    tint = if (torchOn) Color(0xFFFFEB3B) else Color.White
                )
            }
        }

        // 4. Danger strip (pulsing) while reviewing a danger value.
        if (state.phase == ScannerPhase.REVIEW && state.draft?.risk?.isDanger == true) {
            DangerStrip(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
            )
        }

        // 5. Live hint pill while scanning.
        if (state.phase == ScannerPhase.SCANNING) {
            ScanHintPill(
                text = when {
                    state.framesWithoutResult > EMPTY_FRAMES_BEFORE_HINT ->
                        "Chưa đọc được — giữ máy đo yên, tránh lóa rồi thử lại"
                    else -> "Đưa màn hình máy đo vào khung xanh"
                },
                subText = if (state.rejectedNoiseCount > 0) {
                    "AI đã lọc ${state.rejectedNoiseCount} giá trị nhiễu"
                } else {
                    "AI đang quét số • giờ • ngày theo thời gian thực"
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
            )
        }

        // 6. Instant result banner (review → confirm).
        if ((state.phase == ScannerPhase.REVIEW || state.phase == ScannerPhase.SAVING) &&
            state.draft != null
        ) {
            ScanReviewBanner(
                draft = state.draft!!,
                isSaving = state.phase == ScannerPhase.SAVING,
                saveError = state.saveError,
                onConfirm = { viewModel.confirmSave() },
                onRescan = { viewModel.rescan() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // 7. Saved banner (forecast + warnings).
        if (state.phase == ScannerPhase.SAVED && state.draft != null) {
            ScanSavedBanner(
                draft = state.draft!!,
                saveResult = state.saveResult,
                forecast = state.postSaveForecast,
                forecastMessage = state.forecastMessage,
                retrainReady = state.retrainReady,
                onRescan = { viewModel.rescan() },
                onViewDiary = {
                    val date = state.draft!!.date
                    navController.navigate("${GlucoseScreen.DayDetail.route}/$date") {
                        popUpTo(GlucoseScreen.Scanner.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // 8. Danger flash overlay.
        if (showRedFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun ScanHintPill(text: String, subText: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subText,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DangerStrip(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "danger")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dangerAlpha"
    )
    Surface(
        modifier = modifier.alpha(alpha),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFB71C1C)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                text = "CẢNH BÁO NGUY HIỂM",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun ScannerPermissionContent(onBack: () -> Unit, onRequest: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Quay lại", tint = Color.White)
            }
            Text(
                text = "Quét AI Pro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Cần cấp quyền Camera để quét màn hình máy đo.",
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequest) { Text("Cấp quyền camera") }
        }
    }
}

private const val ANALYSIS_INTERVAL_MS = 250L
private const val EMPTY_FRAMES_BEFORE_HINT = 12
