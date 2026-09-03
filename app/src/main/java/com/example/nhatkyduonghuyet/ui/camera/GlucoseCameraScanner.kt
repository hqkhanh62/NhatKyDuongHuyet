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
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.example.nhatkyduonghuyet.ml.ScannerGeometry
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * Shared live camera glucose scanner.
 *
 * Used by both the full-screen ScannerScreen (dashboard) and the
 * CameraScannerDialog (day detail), so both surfaces show the same guide
 * frame size and run the same OCR pipeline.
 *
 * Key accuracy features:
 * - The analyzed crop is exactly the region shown inside the green guide
 *   frame (WYSIWYG), derived from the preview's FILL_CENTER transform.
 * - Tap-to-focus on the meter display.
 * - A value is delivered only after a stability window of agreeing frames.
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
    val recentValues = remember { ArrayDeque<Float>() }
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
    var noResultHint by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            kotlinx.coroutines.delay(SCAN_FEEDBACK_TIMEOUT_MS)
            if (!hasDeliveredResult.get() && errorMessage == null) {
                noResultHint = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef.get()?.unbindAll()
            cameraExecutor.shutdownNow()
        }
    }

    // Preferred guide-frame width: a fixed fraction of the screen width so the
    // dialog scanner and the full-screen scanner show the same frame size.
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
            Text(
                "Cần cấp quyền Camera để quét máy đo.",
                textAlign = TextAlign.Center
            )
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
                    // Tap-to-focus on the meter display for a sharper OCR crop.
                    val previewView = previewViewRef.get() ?: return@detectTapGestures
                    val camera = cameraRef.get() ?: return@detectTapGestures
                    val focusPoint = previewView.meteringPointFactory
                        .createPoint(tapOffset.x, tapOffset.y)
                    camera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(focusPoint)
                            .setAutoCancelDuration(5, TimeUnit.SECONDS)
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

                    val preview = Preview.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val imageAnalysis = ImageAnalysis.Builder()
                        // Higher analysis resolution gives OCR more pixels
                        // for the display digits; CameraX falls back to the
                        // closest supported size on low-end devices.
                        .setTargetResolution(Size(1920, 1080))
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

                                // Crop exactly what the on-screen guide frame
                                // shows: the same geometry the overlay draws,
                                // mapped through the FILL_CENTER transform.
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

                                scanner.processHybrid(
                                    bitmap,
                                    rotationDegrees,
                                    roi,
                                    onResult = { result ->
                                        isProcessing.set(false)
                                        imageProxy.close()
                                        if (result != null && !hasDeliveredResult.get()) {
                                            mainExecutor.execute {
                                                noResultHint = false
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
                        cameraRef.set(camera)
                        onCameraBound?.invoke(camera)
                        val focusFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                        camera.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(focusFactory.createPoint(0.5f, 0.5f))
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                        )
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

        // Dimmed scrim outside the guide frame + the green frame itself.
        guideFrame?.let { frame ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scrimColor = Color.Black.copy(alpha = 0.45f)
                drawRect(
                    color = scrimColor,
                    topLeft = Offset(0f, 0f),
                    size = GeometrySize(size.width, frame.top)
                )
                drawRect(
                    color = scrimColor,
                    topLeft = Offset(0f, frame.bottom),
                    size = GeometrySize(size.width, size.height - frame.bottom)
                )
                drawRect(
                    color = scrimColor,
                    topLeft = Offset(0f, frame.top),
                    size = GeometrySize(frame.left, frame.height)
                )
                drawRect(
                    color = scrimColor,
                    topLeft = Offset(frame.right, frame.top),
                    size = GeometrySize(size.width - frame.right, frame.height)
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
                    if (noResultHint) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Chưa đọc được. Giữ máy đo yên trong khung, tránh lóa rồi thử lại.",
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

private const val ANALYSIS_INTERVAL_MS = 250L
private const val SCAN_FEEDBACK_TIMEOUT_MS = 8_000L
private const val STABILITY_WINDOW_SIZE = 4
private const val STABILITY_REQUIRED_MATCHES = 3
private const val STABILITY_TOLERANCE = 0.15f

private val GuideFrameColor = Color(0xFF4CE067)

/**
 * Returns the most frequent value of the last frames when enough of them
 * agree with the latest one. This protects against a single noisy frame
 * delivering a wrong reading.
 */
private fun findStableValue(values: ArrayDeque<Float>): Float? {
    if (values.size < STABILITY_REQUIRED_MATCHES) return null
    val latest = values.peekLast()
    val matches = values.filter { abs(it - latest) <= STABILITY_TOLERANCE }
    if (matches.size < STABILITY_REQUIRED_MATCHES) return null
    return matches.groupingBy { it }.eachCount()
        .entries.maxByOrNull { it.value }?.key ?: latest
}
