# OCR Fix - Instructions for AI Agent

**Project:** NhatKyDuongHuyet
**Issue:** App crashes/freezes after one scan loop
**Solution:** Refactor OCR logic with proper resource management

---

## 📌 Priority & Scope

- **Priority:** HIGH
- **Files to Create:** 4 new files
- **Files to Modify:** 3 existing files
- **Dependencies to Add:** 8 libraries
- **Estimated Time:** 2-3 hours

---

## 🎯 Objective

Fix the issue where the app crashes or freezes after the scan bar completes one loop. The root cause is:
- Memory leaks in Camera/OCR processing
- Threads not properly cleaned up
- ImageProxy not being closed
- Resource leak from ML Kit
- ANR (Application Not Responding) from heavy processing

---

## 📋 Step-by-Step Instructions

### STEP 1: Create New Package Structure

**Action:** Create the following directory structure if not exists:

\`\`\`
app/src/main/java/com/example/nhatkyduonghuyet/
├── ui/scan/              (already exists)
│   ├── ScanActivity.kt   (MODIFY)
│   ├── ScanViewModel.kt  (MODIFY)
│   ├── CameraManager.kt  (CREATE)
│   └── OCRProcessor.kt   (CREATE)
├── data/ml/              (CREATE if not exists)
│   └── TextRecognitionManager.kt (CREATE)
└── utils/                (already exists)
\`\`\`

**Commands to execute:**
\`\`\`bash
mkdir -p app/src/main/java/com/example/nhatkyduonghuyet/data/ml
\`\`\`

---

### STEP 2: Create File #1 - CameraManager.kt

**File Path:** \`app/src/main/java/com/example/nhatkyduonghuyet/ui/scan/CameraManager.kt\`

**Action:** Create new file with the following content:

\`\`\`kotlin
package com.example.nhatkyduonghuyet.ui.scan

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages camera initialization, lifecycle, and frame processing
 * Handles all camera-related setup and cleanup
 * 
 * KEY FEATURES:
 * - Proper resource cleanup in release()
 * - BackpressureStrategy to prevent frame queue overflow
 * - ImageProxy closed in finally block
 * - Unbind/bind on lifecycle changes
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "CameraManager"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null
    private var analysisCallback: ((imageProxy: ImageProxy) -> Unit)? = null

    init {
        // Create single thread executor for camera processing
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    /**
     * Initialize camera and bind to lifecycle
     * MUST be called before any other methods
     */
    fun initializeCamera(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.result

                val preview = Preview.Builder().build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // ⭐ CRITICAL: Unbind all to prevent conflicts
                cameraProvider?.unbindAll()

                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    getImageAnalysis()
                )

                onSuccess()
                Log.d(TAG, "✓ Camera initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error initializing camera", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Setup ImageAnalysis with proper backpressure handling
     * KEY POINT: STRATEGY_KEEP_ONLY_LATEST prevents frame queue from growing
     */
    private fun getImageAnalysis(): ImageAnalysis {
        imageAnalysis = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            android.util.Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            // ⭐ CRITICAL: Keep only latest frame to prevent backlog
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis!!.setAnalyzer(cameraExecutor!!) { imageProxy ->
            try {
                analysisCallback?.invoke(imageProxy)
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error in image analysis", e)
            } finally {
                // ⭐ CRITICAL: Always close imageProxy to prevent memory leak
                try {
                    imageProxy.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing imageProxy", e)
                }
            }
        }

        return imageAnalysis!!
    }

    fun setAnalysisCallback(callback: (imageProxy: ImageProxy) -> Unit) {
        this.analysisCallback = callback
    }

    fun bindPreview(previewView: androidx.camera.view.PreviewView) {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error binding preview", e)
        }
    }

    fun pauseAnalysis() {
        try {
            cameraProvider?.unbindAll()
            Log.d(TAG, "Camera analysis paused")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error pausing camera", e)
        }
    }

    fun resumeAnalysis() {
        try {
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
            Log.d(TAG, "Camera analysis resumed")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error resuming camera", e)
        }
    }

    /**
     * Release all camera resources
     * ⭐ MUST be called in Activity.onDestroy()
     * This is critical to prevent memory leaks
     */
    fun release() {
        try {
            cameraProvider?.unbindAll()
            camera = null
            imageAnalysis = null
            analysisCallback = null

            // ⭐ CRITICAL: Shutdown executor
            cameraExecutor?.shutdown()
            cameraExecutor = null

            Log.d(TAG, "✓ Camera resources released")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error releasing camera", e)
        }
    }

    fun getCameraProvider() = cameraProvider
    fun getCamera() = camera
}
\`\`\`

---

### STEP 3: Create File #2 - OCRProcessor.kt

**File Path:** \`app/src/main/java/com/example/nhatkyduonghuyet/ui/scan/OCRProcessor.kt\`

**Action:** Create new file with the following content:

\`\`\`kotlin
package com.example.nhatkyduonghuyet.ui.scan

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.vietnamese.TextRecognizerOptions
import kotlinx.coroutines.*

/**
 * Handles OCR (Optical Character Recognition) processing
 * Manages text detection using Google ML Kit
 * 
 * KEY FEATURES:
 * - Timeout handling to prevent hanging
 * - Rate limiting to prevent overprocessing
 * - Proper coroutine cancellation
 * - Resource cleanup (InputImage.close(), etc)
 * - Prevents concurrent processing
 */
class OCRProcessor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "OCRProcessor"
        private const val MIN_CONFIDENCE = 0.7
        private const val PROCESS_TIMEOUT_MS = 5000L
        private const val MIN_PROCESS_INTERVAL = 100L // Prevent overprocessing
    }

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }

    private var isProcessing = false
    private var processingJob: Job? = null
    private var lastProcessedTime = 0L

    var onTextDetected: ((String, Float) -> Unit)? = null
    var onError: ((Exception) -> Unit)? = null
    var onProcessingStateChanged: ((Boolean) -> Unit)? = null

    /**
     * Process image from camera for text recognition
     * 
     * RATE LIMITING:
     * - Skips if MIN_PROCESS_INTERVAL hasn't elapsed
     * - Prevents processing when already busy
     * - Cancels previous job if still running
     */
    fun processImage(imageProxy: ImageProxy, cropRect: Rect? = null) {
        // ⭐ Rate limiting: prevent processing too frequently
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < MIN_PROCESS_INTERVAL) {
            return
        }

        // ⭐ Prevent multiple concurrent processing
        if (isProcessing) {
            Log.d(TAG, "Still processing, skipping")
            return
        }

        // ⭐ Cancel previous processing if still running
        processingJob?.cancel()

        processingJob = scope.launch(Dispatchers.Default) {
            try {
                isProcessing = true
                withContext(Dispatchers.Main) {
                    onProcessingStateChanged?.invoke(true)
                }

                lastProcessedTime = currentTime

                val image = imageProxy.image ?: run {
                    Log.w(TAG, "Image is null")
                    return@launch
                }

                val inputImage = InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )

                // Crop if needed
                val processImage = if (cropRect != null) {
                    inputImage.getImage()?.let { bitmap ->
                        try {
                            android.graphics.Bitmap.createBitmap(
                                bitmap,
                                cropRect.left.coerceIn(0, bitmap.width - 1),
                                cropRect.top.coerceIn(0, bitmap.height - 1),
                                cropRect.width().coerceAtMost(bitmap.width),
                                cropRect.height().coerceAtMost(bitmap.height)
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Error cropping", e)
                            bitmap
                        }
                    }?.let {
                        InputImage.fromBitmap(it, inputImage.rotationDegrees)
                    } ?: inputImage
                } else {
                    inputImage
                }

                // ⭐ Recognize text WITH TIMEOUT
                val task = textRecognizer.process(processImage)

                val visionText = withTimeoutOrNull(PROCESS_TIMEOUT_MS) {
                    suspendCancellableCoroutine { continuation ->
                        task.addOnSuccessListener { text ->
                            if (continuation.isActive) {
                                continuation.resume(text)
                            }
                        }
                        .addOnFailureListener { e ->
                            if (continuation.isActive) {
                                continuation.resumeWithException(e)
                            }
                        }
                    }
                } ?: run {
                    Log.w(TAG, "OCR timeout")
                    return@launch
                }

                // Process results
                val detectedText = visionText.text
                val confidence = calculateConfidence(visionText.blocks)

                Log.d(TAG, "✓ Detected: '$detectedText', confidence: $confidence")

                withContext(Dispatchers.Main) {
                    if (detectedText.isNotEmpty() && confidence >= MIN_CONFIDENCE) {
                        onTextDetected?.invoke(detectedText, confidence)
                    }
                }

                // ⭐ Cleanup resources
                processImage.close()
                inputImage.close()

            } catch (e: CancellationException) {
                Log.d(TAG, "OCR processing cancelled")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error processing image", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            } finally {
                isProcessing = false
                withContext(Dispatchers.Main) {
                    onProcessingStateChanged?.invoke(false)
                }
            }
        }
    }

    private fun calculateConfidence(blocks: List<com.google.mlkit.vision.text.Text.TextBlock>): Float {
        if (blocks.isEmpty()) return 0f

        var totalConfidence = 0f
        var count = 0

        blocks.forEach { block ->
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    totalConfidence += element.confidence
                    count++
                }
            }
        }

        return if (count > 0) totalConfidence / count else 0f
    }

    fun stopProcessing() {
        processingJob?.cancel()
        isProcessing = false
    }

    /**
     * Release all resources
     * ⭐ MUST be called in ViewModel.onCleared()
     */
    fun release() {
        try {
            stopProcessing()
            textRecognizer.close()
            onTextDetected = null
            onError = null
            onProcessingStateChanged = null
            Log.d(TAG, "✓ OCR processor released")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error releasing OCR processor", e)
        }
    }

    fun isCurrentlyProcessing() = isProcessing
}
\`\`\`

---

### STEP 4: Create File #3 - TextRecognitionManager.kt

**File Path:** \`app/src/main/java/com/example/nhatkyduonghuyet/data/ml/TextRecognitionManager.kt\`

**Action:** Create new file with the following content:

\`\`\`kotlin
package com.example.nhatkyduonghuyet.data.ml

import android.content.Context
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.vietnamese.TextRecognizerOptions

/**
 * Singleton manager for ML Kit text recognition
 * Handles model downloads and recognizer lifecycle
 */
class TextRecognitionManager(private val context: Context) {
    companion object {
        private const val TAG = "TextRecognitionManager"
        private var instance: TextRecognitionManager? = null

        fun getInstance(context: Context): TextRecognitionManager {
            return instance ?: synchronized(this) {
                TextRecognitionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }

    init {
        downloadModels()
    }

    private fun downloadModels() {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage("vi")
                .setTargetLanguage("vi")
                .build()
        ).downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.d(TAG, "✓ Models downloaded")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Models download failed: ${e.message}")
            }
    }

    fun getRecognizer() = textRecognizer

    fun release() {
        try {
            textRecognizer.close()
            instance = null
            Log.d(TAG, "✓ Recognizer released")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error releasing", e)
        }
    }
}
\`\`\`

---

### STEP 5: Modify File #1 - ScanViewModel.kt

**File Path:** \`app/src/main/java/com/example/nhatkyduonghuyet/ui/scan/ScanViewModel.kt\`

**Action:** Replace entire content with:

\`\`\`kotlin
package com.example.nhatkyduonghuyet.ui.scan

import android.app.Application
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nhatkyduonghuyet.data.ml.TextRecognitionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for scan activity
 * Manages OCR processing and UI state
 * Handles lifecycle-aware resource management
 * 
 * KEY POINT: onCleared() is called when Activity is destroyed
 * This is where we cleanup resources
 */
class ScanViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ScanViewModel"
    }

    private val textRecognitionManager = TextRecognitionManager.getInstance(application)
    private var ocrProcessor: OCRProcessor? = null

    private val _scannedText = MutableLiveData<String>()
    val scannedText: LiveData<String> = _scannedText

    private val _textConfidence = MutableLiveData<Float>()
    val textConfidence: LiveData<Float> = _textConfidence

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _scanError = MutableLiveData<String>()
    val scanError: LiveData<String> = _scanError

    private val _scanState = MutableLiveData<ScanState>(ScanState.IDLE)
    val scanState: LiveData<ScanState> = _scanState

    enum class ScanState {
        IDLE, SCANNING, PROCESSING, SUCCESS, ERROR
    }

    /**
     * Initialize OCR processor
     * Called in Activity.onCreate()
     */
    fun initializeOCR() {
        if (ocrProcessor != null) {
            Log.d(TAG, "OCR already initialized")
            return
        }

        ocrProcessor = OCRProcessor(getApplication(), viewModelScope).apply {
            onTextDetected = { text, confidence ->
                Log.d(TAG, "✓ Text detected: '$text'")
                _scannedText.value = text
                _textConfidence.value = confidence
                _scanState.value = ScanState.SUCCESS
            }

            onError = { error ->
                Log.e(TAG, "✗ OCR Error: ${error.message}", error)
                _scanError.value = error.message ?: "Unknown error"
                _scanState.value = ScanState.ERROR
            }

            onProcessingStateChanged = { isProcessing ->
                _isProcessing.value = isProcessing
                if (isProcessing) {
                    _scanState.value = ScanState.PROCESSING
                }
            }
        }
        Log.d(TAG, "✓ OCR initialized")
    }

    /**
     * Process frame from camera
     */
    fun processFrame(imageProxy: ImageProxy, cropRect: Rect? = null) {
        val currentState = _scanState.value
        
        if (currentState == ScanState.IDLE || currentState == ScanState.SCANNING) {
            _scanState.value = ScanState.SCANNING
            ocrProcessor?.processImage(imageProxy, cropRect)
        }
    }

    fun resetScan() {
        _scannedText.value = ""
        _textConfidence.value = 0f
        _scanState.value = ScanState.IDLE
        _scanError.value = ""
        ocrProcessor?.stopProcessing()
        Log.d(TAG, "✓ Scan reset")
    }

    fun pauseScan() {
        ocrProcessor?.stopProcessing()
        Log.d(TAG, "Scan paused")
    }

    fun resumeScan() {
        _scanState.value = ScanState.IDLE
        Log.d(TAG, "Scan resumed")
    }

    /**
     * ⭐ CRITICAL: Cleanup when ViewModel is destroyed
     * This is called when Activity.onDestroy() is executed
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "✓ ViewModel onCleared() - Cleaning up resources")
        
        ocrProcessor?.release()
        ocrProcessor = null
        textRecognitionManager.release()
    }
}
\`\`\`

---

### STEP 6: Modify File #2 - ScanActivity.kt

**File Path:** \`app/src/main/java/com/example/nhatkyduonghuyet/ui/scan/ScanActivity.kt\`

**Action:** Replace entire content with:

\`\`\`kotlin
package com.example.nhatkyduonghuyet.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.nhatkyduonghuyet.databinding.ActivityScanBinding

/**
 * Activity for camera-based OCR scanning
 * 
 * KEY LIFECYCLE POINTS:
 * - onCreate(): Initialize camera, OCR, UI
 * - onPause(): Pause camera analysis
 * - onResume(): Resume camera analysis
 * - onDestroy(): CRITICAL - Release camera resources
 */
class ScanActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ScanActivity"
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    private lateinit var binding: ActivityScanBinding
    private val viewModel: ScanViewModel by viewModels()
    private var cameraManager: CameraManager? = null
    private var hasCameraPermission = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            initializeCamera()
        } else {
            showError("Camera permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "✓ Activity created")

        setupUI()
        setupViewModel()
        requestCameraPermission()
    }

    private fun setupUI() {
        binding.apply {
            btnReset.setOnClickListener {
                Log.d(TAG, "Reset clicked")
                viewModel.resetScan()
            }

            btnClose.setOnClickListener {
                Log.d(TAG, "Close clicked")
                finish()
            }
        }
    }

    private fun setupViewModel() {
        viewModel.apply {
            initializeOCR()

            scannedText.observe(this@ScanActivity) { text ->
                binding.tvScannedText.text = text
            }

            textConfidence.observe(this@ScanActivity) { confidence ->
                binding.tvConfidence.text = String.format("Confidence: %.1f%%", confidence * 100)
            }

            isProcessing.observe(this@ScanActivity) { isProcessing ->
                updateProcessingUI(isProcessing)
            }

            scanState.observe(this@ScanActivity) { state ->
                Log.d(TAG, "State: $state")
            }

            scanError.observe(this@ScanActivity) { error ->
                if (error.isNotEmpty()) {
                    showError(error)
                }
            }
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                CAMERA_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasCameraPermission = true
                initializeCamera()
            }
            shouldShowRequestPermissionRationale(CAMERA_PERMISSION) -> {
                showError("Camera permission required")
                permissionLauncher.launch(CAMERA_PERMISSION)
            }
            else -> {
                permissionLauncher.launch(CAMERA_PERMISSION)
            }
        }
    }

    private fun initializeCamera() {
        cameraManager = CameraManager(this, this).apply {
            initializeCamera(
                onSuccess = {
                    Log.d(TAG, "✓ Camera initialized")
                    bindPreview(binding.previewView)

                    setAnalysisCallback { imageProxy ->
                        val cropRect = calculateScanRect()
                        viewModel.processFrame(imageProxy, cropRect)
                    }
                },
                onError = { error ->
                    Log.e(TAG, "✗ Camera error", error)
                    showError("Camera error: ${error.message}")
                }
            )
        }
    }

    private fun calculateScanRect(): Rect {
        val previewWidth = binding.previewView.width
        val previewHeight = binding.previewView.height

        val width = (previewWidth * 0.8).toInt()
        val height = (previewHeight * 0.3).toInt()
        val left = (previewWidth - width) / 2
        val top = (previewHeight - height) / 2

        return Rect(left, top, left + width, top + height)
    }

    private fun updateProcessingUI(isProcessing: Boolean) {
        binding.progressBar.visibility = if (isProcessing) View.VISIBLE else View.GONE
        binding.tvProcessing.visibility = if (isProcessing) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")
        cameraManager?.pauseAnalysis()
        viewModel.pauseScan()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
        if (hasCameraPermission) {
            cameraManager?.resumeAnalysis()
            viewModel.resumeScan()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "✓ Activity destroyed - RELEASING RESOURCES")
        
        // ⭐ CRITICAL: Release camera resources
        cameraManager?.release()
        cameraManager = null
    }
}
\`\`\`

---

### STEP 7: Modify File #3 - build.gradle

**File Path:** \`app/build.gradle\` or \`app/build.gradle.kts\`

**Action:** Add these dependencies to the \`dependencies\` block:

\`\`\`gradle
// CameraX
implementation "androidx.camera:camera-core:1.3.0"
implementation "androidx.camera:camera-camera2:1.3.0"
implementation "androidx.camera:camera-lifecycle:1.3.0"
implementation "androidx.camera:camera-view:1.3.0"
implementation "androidx.camera:camera-extensions:1.3.0"

// ML Kit - Text Recognition
implementation "com.google.mlkit:text-recognition:16.0.0"
implementation "com.google.mlkit:text-recognition-vietnamese:16.0.0"

// ML Kit - Translator (for model management)
implementation "com.google.mlkit:translate:17.0.0"

// Coroutines
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1"

// Lifecycle
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1"
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.1"
implementation "androidx.lifecycle:lifecycle-viewmodel:2.6.1"

// UI
implementation "androidx.appcompat:appcompat:1.6.1"
implementation "com.google.android.material:material:1.9.0"

// Debugging (optional)
debugImplementation "com.squareup.leakcanary:leakcanary-android:2.12"
\`\`\`

---

### STEP 8: Update activity_scan.xml

**File Path:** \`app/src/main/res/layout/activity_scan.xml\`

**Action:** Replace with:

\`\`\`xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@android:color/black">

    <androidx.camera.view.PreviewView
        android:id="@+id/previewView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp"
        android:background="#80000000">

        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="match_parent"
            android:layout_height="4dp"
            android:visibility="gone"
            style="?android:attr/progressBarStyleHorizontal" />

        <TextView
            android:id="@+id/tvProcessing"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Processing..."
            android:textColor="@android:color/holo_orange_light"
            android:textAlignment="center"
            android:textSize="12sp"
            android:visibility="gone" />

        <TextView
            android:id="@+id/tvConfidence"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Confidence: 0%"
            android:textColor="@android:color/holo_green_light"
            android:textAlignment="center"
            android:textSize="12sp"
            android:layout_marginTop="8dp" />

        <TextView
            android:id="@+id/tvScannedText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Scanned text will appear here"
            android:textColor="@android:color/white"
            android:textAlignment="center"
            android:textSize="16sp"
            android:layout_marginTop="12dp"
            android:minHeight="60dp"
            android:padding="8dp" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="16dp"
            android:gravity="center">

            <Button
                android:id="@+id/btnReset"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Reset"
                android:layout_marginEnd="8dp" />

            <Button
                android:id="@+id/btnClose"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Close" />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
\`\`\`

---

### STEP 9: Update AndroidManifest.xml

**File Path:** \`app/src/main/AndroidManifest.xml\`

**Action:** Add these permissions inside \`<manifest>\` tag:

\`\`\`xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<uses-feature
    android:name="android.hardware.camera"
    android:required="false" />
<uses-feature
    android:name="android.hardware.camera.autofocus"
    android:required="false" />
\`\`\`

**Action:** Update ScanActivity definition:

\`\`\`xml
<activity
    android:name=".ui.scan.ScanActivity"
    android:screenOrientation="portrait"
    android:exported="false" />
\`\`\`

---

## 🚀 Execution Instructions

### Command Sequence (for AI Agent):

\`\`\`bash
# 1. Create directories
mkdir -p app/src/main/java/com/example/nhatkyduonghuyet/data/ml

# 2. Create new files with content from STEP 2, 3, 4
# File 1: CameraManager.kt
# File 2: OCRProcessor.kt  
# File 3: TextRecognitionManager.kt

# 3. Modify existing files with content from STEP 5, 6
# File 1: ScanViewModel.kt (REPLACE entire content)
# File 2: ScanActivity.kt (REPLACE entire content)

# 4. Update configuration files
# build.gradle (ADD dependencies)
# activity_scan.xml (REPLACE)
# AndroidManifest.xml (UPDATE)

# 5. Sync & Build
./gradlew clean
./gradlew build

# 6. Install & Test
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat | grep -E "ScanActivity|CameraManager|OCRProcessor"