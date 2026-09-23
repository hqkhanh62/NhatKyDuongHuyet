package com.example.nhatkyduonghuyet.ui.scanner

import android.util.Size
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.ml.MeterDisplayFields
import com.example.nhatkyduonghuyet.ml.SCAN_FRAME_ASPECT_RATIO
import com.example.nhatkyduonghuyet.ml.ScannedGlucoseResult
import com.example.nhatkyduonghuyet.ml.scanRoiForViewport
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/** Analysis cadence; slower than the preview to leave CPU for ML Kit. */
const val SCAN_ANALYSIS_INTERVAL_MS = 250L

/**
 * Auto-focus/metering is re-armed this often. The initial request
 * auto-cancels after a few seconds; a handheld meter at 15-20 cm then defocuses
 * and every later frame analyses blurred digits - a major source of misreads.
 */
const val FOCUS_REFRESH_INTERVAL_MS = 2_500L

/**
 * Camera preview + green guide frame + hybrid scanning loop, shared by the
 * full-screen scanner and the DayDetail dialog.
 *
 * The guide frame keeps a fixed aspect ratio and its measured on-screen size is
 * translated into the analysis-frame ROI, so both entry points feed the OCR the
 * exact same framing of the meter display. Previously each screen drew a
 * different-sized frame while the analyser always cropped a fixed 0.20..0.80
 * box, which is why the small dialog frame produced much worse readings.
 */
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun GlucoseCameraPreview(
    scanner: GlucoseScanner,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    torchEnabled: Boolean = false,
    onCameraReady: (CameraControl) -> Unit = {},
    onError: (Exception) -> Unit = {},
    onOcrFields: (MeterDisplayFields) -> Unit = {},
    overlay: @Composable BoxScope.(ScanFrameSpec) -> Unit = { spec -> ScanGuideFrame(spec) },
    onResult: (ScannedGlucoseResult) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isProcessing = remember { AtomicBoolean(false) }
    val stopped = remember { AtomicBoolean(false) }
    val lastAttemptAt = remember { AtomicLong(0L) }
    val cameraProviderRef = remember { AtomicReference<ProcessCameraProvider?>(null) }
    val cameraControlRef = remember { AtomicReference<CameraControl?>(null) }
    val focusActionRef = remember { AtomicReference<FocusMeteringAction?>(null) }
    val lastFocusRequestAt = remember { AtomicLong(0L) }
    val previewViewRef = remember { AtomicReference<PreviewView?>(null) }
    // The analyzer lambda is created once, so state it reads must live in refs.
    val enabledRef = remember { AtomicBoolean(enabled) }
    enabledRef.set(enabled)

    DisposableEffect(Unit) {
        onDispose {
            stopped.set(true)
            cameraProviderRef.get()?.unbindAll()
            cameraExecutor.shutdownNow()
        }
    }

    DisposableEffect(torchEnabled) {
        cameraControlRef.get()?.enableTorch(torchEnabled)
        onDispose { }
    }

    BoxWithConstraints(modifier = modifier) {
        val viewWidthPx = with(density) { maxWidth.toPx() }
        val viewHeightPx = with(density) { maxHeight.toPx() }

        // Guide frame: same shape everywhere, ~82% of the shortest usable side.
        val frameWidthPx = minOf(viewWidthPx * 0.86f, viewHeightPx * 0.86f * SCAN_FRAME_ASPECT_RATIO)
        val frameHeightPx = frameWidthPx / SCAN_FRAME_ASPECT_RATIO
        val frameWidthDp = with(density) { frameWidthPx.toDp() }
        val frameHeightDp = with(density) { frameHeightPx.toDp() }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewViewRef.set(previewView)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = try {
                        cameraProviderFuture.get()
                    } catch (e: Exception) {
                        onError(e)
                        return@addListener
                    }
                    cameraProviderRef.set(provider)

                    val preview = Preview.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val imageAnalysis = ImageAnalysis.Builder()
                        // Higher analysis resolution: digit strokes survive the crop.
                        .setTargetResolution(Size(1920, 1080))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val now = System.currentTimeMillis()
                                val shouldAnalyze = enabledRef.get() && !stopped.get() &&
                                    !isProcessing.get() &&
                                    now - lastAttemptAt.get() >= SCAN_ANALYSIS_INTERVAL_MS

                                if (!shouldAnalyze) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                lastAttemptAt.set(now)
                                if (!isProcessing.compareAndSet(false, true)) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                // Keep the guide frame in focus for the whole
                                // scan (CameraControl is thread-safe).
                                val lastFocus = lastFocusRequestAt.get()
                                if (now - lastFocus >= FOCUS_REFRESH_INTERVAL_MS &&
                                    lastFocusRequestAt.compareAndSet(lastFocus, now)
                                ) {
                                    focusActionRef.get()?.let { action ->
                                        cameraControlRef.get()?.startFocusAndMetering(action)
                                    }
                                }

                                val bitmap = try {
                                    imageProxy.toBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                                if (bitmap == null) {
                                    isProcessing.set(false)
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val rotation = imageProxy.imageInfo.rotationDegrees
                                // Rotated frame dimensions, matching what is displayed.
                                val upright = rotation == 90 || rotation == 270
                                val imageWidth = if (upright) bitmap.height else bitmap.width
                                val imageHeight = if (upright) bitmap.width else bitmap.height

                                val roi = scanRoiForViewport(
                                    imageWidth = imageWidth,
                                    imageHeight = imageHeight,
                                    viewWidth = viewWidthPx.toInt(),
                                    viewHeight = viewHeightPx.toInt(),
                                    frameWidth = frameWidthPx,
                                    frameHeight = frameHeightPx
                                )

                                scanner.processHybrid(
                                    bitmap,
                                    rotation,
                                    roi,
                                    onResult = { result ->
                                        isProcessing.set(false)
                                        imageProxy.close()
                                        if (result != null && !stopped.get()) onResult(result)
                                    },
                                    onError = {
                                        isProcessing.set(false)
                                        imageProxy.close()
                                    },
                                    onOcrFields = { fields ->
                                        if (!stopped.get()) onOcrFields(fields)
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
                        cameraControlRef.set(camera.cameraControl)
                        onCameraReady(camera.cameraControl)
                        // Focus and expose on the guide frame itself, not the
                        // whole scene: a bright room next to a dim LCD used to
                        // wash the digits out. The action is kept and re-armed
                        // from the analyzer (see FOCUS_REFRESH_INTERVAL_MS) so
                        // the meter stays sharp for the whole scan.
                        previewView.post {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(
                                previewView.width / 2f,
                                previewView.height / 2f,
                                (frameWidthPx / maxOf(previewView.width, 1)).coerceIn(0.1f, 1f)
                            )
                            val action = FocusMeteringAction.Builder(
                                point,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            )
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            focusActionRef.set(action)
                            lastFocusRequestAt.set(System.currentTimeMillis())
                            camera.cameraControl.startFocusAndMetering(action)
                        }
                    } catch (error: Exception) {
                        onError(error)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    detectTapGestures { tap ->
                        requestFocusAt(
                            tap = tap,
                            previewView = previewViewRef.get(),
                            cameraControl = cameraControlRef.get(),
                            focusActionRef = focusActionRef,
                            lastFocusRequestAt = lastFocusRequestAt
                        )
                    }
                }
        )

        overlay(
            ScanFrameSpec(
                viewWidth = maxWidth,
                viewHeight = maxHeight,
                frameWidth = frameWidthDp,
                frameHeight = frameHeightDp
            )
        )
    }
}

/**
 * Chạm vào màn hình để lấy nét đúng điểm người dùng chọn (tap-to-focus).
 * Hành động focus được lưu lại vào [focusActionRef] nên vòng lặp cấp lại
 * tiêu cự mỗi ~2.5 giây sẽ giữ nguyên điểm người dùng chạm thay vì nhảy về
 * giữa khung.
 */
private fun requestFocusAt(
    tap: Offset,
    previewView: PreviewView?,
    cameraControl: CameraControl?,
    focusActionRef: AtomicReference<FocusMeteringAction?>,
    lastFocusRequestAt: AtomicLong
) {
    if (previewView == null || cameraControl == null) return
    val action = runCatching {
        val point = previewView.meteringPointFactory.createPoint(tap.x, tap.y)
        FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(TAP_FOCUS_AUTO_CANCEL_SECONDS, TimeUnit.SECONDS)
            .build()
    }.getOrNull() ?: return
    focusActionRef.set(action)
    lastFocusRequestAt.set(System.currentTimeMillis())
    runCatching { cameraControl.startFocusAndMetering(action) }
}

private const val TAP_FOCUS_AUTO_CANCEL_SECONDS = 6L
