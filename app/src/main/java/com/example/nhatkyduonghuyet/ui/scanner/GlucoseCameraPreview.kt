package com.example.nhatkyduonghuyet.ui.scanner

import android.util.Size
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
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

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
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
                        // wash the digits out.
                        previewView.post {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(
                                previewView.width / 2f,
                                previewView.height / 2f,
                                (frameWidthPx / maxOf(previewView.width, 1)).coerceIn(0.1f, 1f)
                            )
                            camera.cameraControl.startFocusAndMetering(
                                FocusMeteringAction.Builder(
                                    point,
                                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                )
                                    .setAutoCancelDuration(2, TimeUnit.SECONDS)
                                    .build()
                            )
                        }
                    } catch (error: Exception) {
                        onError(error)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .width(frameWidthDp)
                .aspectRatio(SCAN_FRAME_ASPECT_RATIO),
            color = Color.Transparent,
            border = BorderStroke(2.dp, Color.Green.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(8.dp)
        ) {}
    }
}
