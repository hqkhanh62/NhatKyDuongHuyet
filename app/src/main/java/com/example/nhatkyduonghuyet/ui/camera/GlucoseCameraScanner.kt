package com.example.nhatkyduonghuyet.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.MeterStatus
import com.example.nhatkyduonghuyet.ml.StabilityVoter
import com.example.nhatkyduonghuyet.ml.ScanOutcome
import com.example.nhatkyduonghuyet.ml.ScanRejection
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.example.nhatkyduonghuyet.ml.ScannerGeometry
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared live camera glucose scanner used by the full-screen scanner and the entry dialog.
 *
 * Accuracy-relevant behaviour:
 *  * Preview and analysis are bound in one [UseCaseGroup] with the preview's view port, so the
 *    crop that is analysed really is what the green frame shows.
 *  * Frames are rejected with a reason (blur, glare, flat) and the reason is shown to the user.
 *  * A value is delivered only after [STABILITY_REQUIRED_MATCHES] frames report exactly the
 *    same number and no other value contradicts them inside the window.
 */
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun GlucoseCameraScanner(
    scanner: GlucoseScanner,
    modifier: Modifier = Modifier,
    onResult: (ScannedGlucoseResult) -> Unit,
    onCameraBound: ((Camera) -> Unit)? = null,
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isProcessing = remember { AtomicBoolean(false) }
    val hasDeliveredResult = remember { AtomicBoolean(false) }
    val lastAttemptAt = remember { AtomicLong(0L) }
    val voter = remember { StabilityVoter() }
    val cameraRef = remember { AtomicReference<Camera?>(null) }
    val previewViewRef = remember { AtomicReference<PreviewView?>(null) }
    val cameraProviderRef = remember { AtomicReference<ProcessCameraProvider?>(null) }
    val viewSizeRef = remember { AtomicReference(IntSize.Zero) }
    val preferredFrameWidthRef = remember { AtomicReference(0f) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var resultDelivered by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Re-trigger auto focus periodically: CameraX cancels a focus action after a few seconds
    // and a meter held by hand drifts out of focus, which used to produce garbage readings.
    LaunchedEffect(permissionGranted) {
        while (permissionGranted && !hasDeliveredResult.get()) {
            kotlinx.coroutines.delay(REFOCUS_INTERVAL_MS)
            val previewView = previewViewRef.get() ?: continue
            val camera = cameraRef.get() ?: continue
            if (previewView.width <= 0 || previewView.height <= 0) continue
            runCatching {
                val point = previewView.meteringPointFactory
                    .createPoint(previewView.width / 2f, previewView.height / 2f)
                camera.cameraControl.startFocusAndMetering(
                    FocusMeteringAction.Builder(point)
                        .setAutoCancelDuration(REFOCUS_HOLD_SECONDS, TimeUnit.SECONDS)
                        .build()
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef.get()?.unbindAll()
            cameraExecutor.shutdownNow()
        }
    }

    val preferredFrameWidthPx = with(density) {
        ScannerGeometry.preferredFrameWidth(configuration.screenWidthDp.toFloat()).dp.toPx()
    }
    SideEffect { preferredFrameWidthRef.set(preferredFrameWidthPx) }

    val guideFrame = remember(viewSize, preferredFrameWidthPx) {
        ScannerGeometry.computeGuideFrame(
            viewSize.width.toFloat(),
            viewSize.height.toFloat(),
            preferredFrameWidthPx
        )
    }

    if (!permissionGranted) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Cần cấp quyền Camera để quét máy đo.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Cấp quyền camera")
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                viewSize = coordinates.size
                viewSizeRef.set(coordinates.size)
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val previewView = previewViewRef.get() ?: return@detectTapGestures
                    val camera = cameraRef.get() ?: return@detectTapGestures
                    val focusPoint = previewView.meteringPointFactory
                        .createPoint(tapOffset.x, tapOffset.y)
                    camera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(focusPoint)
                            .setAutoCancelDuration(REFOCUS_HOLD_SECONDS, TimeUnit.SECONDS)
                            .build()
                    )
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewViewRef.set(previewView)

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    cameraProviderRef.set(provider)

                    // One resolution strategy for both use cases: if preview and analysis had
                    // different aspect ratios the analysed crop would not match the frame the
                    // user sees, and OCR would read the wrong part of the display.
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1920, 1080),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()

                    val preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val now = System.currentTimeMillis()
                                val shouldAnalyze = !hasDeliveredResult.get() &&
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

                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                val uprightWidth = if (rotationDegrees % 180 != 0) {
                                    bitmap.height
                                } else {
                                    bitmap.width
                                }
                                val uprightHeight = if (rotationDegrees % 180 != 0) {
                                    bitmap.width
                                } else {
                                    bitmap.height
                                }

                                val analyzerViewSize = viewSizeRef.get()
                                val roi = ScannerGeometry.computeGuideFrame(
                                    analyzerViewSize.width.toFloat(),
                                    analyzerViewSize.height.toFloat(),
                                    preferredFrameWidthRef.get()
                                )?.let { frame ->
                                    ScannerGeometry.frameToImageRoi(
                                        frame,
                                        analyzerViewSize.width.toFloat(),
                                        analyzerViewSize.height.toFloat(),
                                        uprightWidth.toFloat(),
                                        uprightHeight.toFloat()
                                    )
                                }

                                scanner.processFrame(
                                    bitmap,
                                    rotationDegrees,
                                    roi,
                                    onOutcome = { outcome ->
                                        isProcessing.set(false)
                                        imageProxy.close()
                                        if (hasDeliveredResult.get()) return@processFrame
                                        mainExecutor.execute {
                                            when (outcome) {
                                                is ScanOutcome.Reading -> {
                                                    hint = null
                                                    val stable = synchronized(voter) {
                                                        voter.offer(outcome.result.value)
                                                    }
                                                    if (stable != null &&
                                                        hasDeliveredResult.compareAndSet(false, true)
                                                    ) {
                                                        resultDelivered = true
                                                        onResult(outcome.result.copy(value = stable))
                                                    }
                                                }

                                                is ScanOutcome.Status -> {
                                                    synchronized(voter) { voter.clear() }
                                                    hint = statusHint(outcome.status)
                                                }

                                                is ScanOutcome.Rejected -> {
                                                    // An unusable frame must not leave stale
                                                    // votes behind for the next reading.
                                                    if (outcome.reason != ScanRejection.NOT_FOUND) {
                                                        synchronized(voter) { voter.clear() }
                                                    }
                                                    hint = rejectionHint(outcome.reason)
                                                }
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
                        val useCaseGroup = UseCaseGroup.Builder()
                            .addUseCase(preview)
                            .addUseCase(imageAnalysis)
                            .apply { previewView.viewPort?.let { setViewPort(it) } }
                            .build()
                        val camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            useCaseGroup
                        )
                        cameraRef.set(camera)
                        onCameraBound?.invoke(camera)
                        // A lit LCD is brighter than its surroundings; a small negative bias
                        // keeps the digits from burning out.
                        runCatching { camera.cameraControl.setExposureCompensationIndex(-1) }
                    } catch (error: Exception) {
                        Log.e("GlucoseCameraScanner", "Camera binding failed", error)
                        mainExecutor.execute {
                            errorMessage = "Không thể mở camera: " +
                                (error.localizedMessage ?: "lỗi không xác định")
                        }
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        guideFrame?.let { frame ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scrimColor = Color.Black.copy(alpha = 0.45f)
                drawRect(scrimColor, Offset(0f, 0f), GeometrySize(size.width, frame.top))
                drawRect(
                    scrimColor,
                    Offset(0f, frame.bottom),
                    GeometrySize(size.width, size.height - frame.bottom)
                )
                drawRect(scrimColor, Offset(0f, frame.top), GeometrySize(frame.left, frame.height))
                drawRect(
                    scrimColor,
                    Offset(frame.right, frame.top),
                    GeometrySize(size.width - frame.right, frame.height)
                )
                drawRoundRect(
                    color = GuideFrameColor,
                    topLeft = Offset(frame.left, frame.top),
                    size = GeometrySize(frame.width, frame.height),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            shape = MaterialTheme.shapes.small,
            color = Color.Black.copy(alpha = 0.55f)
        ) {
            Button(
                onClick = {
                    val camera = cameraRef.get() ?: return@Button
                    torchOn = !torchOn
                    runCatching { camera.cameraControl.enableTorch(torchOn) }
                },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(
                    if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (torchOn) "Tắt đèn" else "Bật đèn"
                )
                Spacer(modifier = Modifier.height(0.dp))
                Text(if (torchOn) " Tắt đèn" else " Bật đèn")
            }
        }

        if (!resultDelivered) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                errorMessage?.let { message ->
                    Text(
                        message,
                        color = Color(0xFFFF8A80),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                } ?: run {
                    Text(
                        "Đưa màn hình máy đo vào khung xanh và giữ yên",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Chạm vào màn hình máy đo để lấy nét",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    hint?.let { message ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            message,
                            color = Color(0xFFFFD54F),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        overlayContent()
    }
}

private const val ANALYSIS_INTERVAL_MS = 220L
private const val REFOCUS_INTERVAL_MS = 2_500L
private const val REFOCUS_HOLD_SECONDS = 3L

private val GuideFrameColor = Color(0xFF4CE067)

private fun rejectionHint(reason: ScanRejection): String = when (reason) {
    ScanRejection.BLURRED -> "Ảnh bị mờ — giữ máy yên và chạm để lấy nét."
    ScanRejection.GLARE -> "Bị loá — nghiêng máy đo hoặc tránh nguồn sáng."
    ScanRejection.LOW_CONTRAST -> "Chưa thấy màn hình máy đo trong khung."
    ScanRejection.DISAGREEMENT -> "Chưa đọc chắc chắn — giữ yên thêm một chút."
    ScanRejection.NOT_FOUND -> "Chưa đọc được chỉ số — đưa dãy số vào giữa khung."
}

private fun statusHint(status: MeterStatus): String = when (status) {
    MeterStatus.HIGH -> "Máy báo HI (đường huyết rất cao) — hãy nhập tay và liên hệ bác sĩ."
    MeterStatus.LOW -> "Máy báo LO (đường huyết rất thấp) — xử trí hạ đường huyết ngay."
    MeterStatus.ERROR -> "Máy đang báo lỗi — kiểm tra que thử rồi đo lại."
}
