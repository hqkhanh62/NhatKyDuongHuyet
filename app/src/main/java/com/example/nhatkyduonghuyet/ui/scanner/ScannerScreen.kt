package com.example.nhatkyduonghuyet.ui.scanner

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
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
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    navController: androidx.navigation.NavController,
    scanner: GlucoseScanner,
    onGlucoseDetected: (Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var detectedValue by remember { mutableStateOf<Float?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val permissionState = remember { mutableStateOf(false) }

    // Request Camera Permission
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionState.value = isGranted
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quét máy đo Glucose") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                                        if (mediaImage != null && !isProcessing) {
                                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                            isProcessing = true
                                            scanner.processImage(
                                                image,
                                                onSuccess = { value ->
                                                    detectedValue = value
                                                    isProcessing = false
                                                    // Trigger callback
                                                    onGlucoseDetected(value)
                                                },
                                                onError = {
                                                    isProcessing = false
                                                }
                                            )
                                        }
                                        imageProxy.close()
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("Scanner", "Binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay UI
                ScannerOverlay()

                // Result Banner
                detectedValue?.let {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Phát hiện chỉ số", style = MaterialTheme.typography.labelSmall)
                                Text("${it} mmol/L", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            }
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Xác nhận")
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cần cấp quyền Camera để sử dụng tính năng này")
                }
            }
        }
    }
}

@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Semi-transparent background with a clear hole in the middle
        Canvas(modifier = Modifier.fillMaxSize()) {
            // This is a simplified version, in real app we'd use path with fillType = EvenOdd
        }
        
        // Guidance text
        Text(
            "Căn giữa số trên màn hình máy đo",
            modifier = Modifier.align(Alignment.Center).padding(top = 200.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
