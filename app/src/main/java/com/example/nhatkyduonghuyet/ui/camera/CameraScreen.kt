package com.example.nhatkyduonghuyet.ui.camera

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CameraScreen() {
    Box(Modifier.fillMaxSize()) {
        // Placeholder for CameraPreview (CameraX)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.Black)
        ) {
            Text("Camera Preview", modifier = Modifier.align(Alignment.Center))
        }

        Box(
            Modifier
                .align(Alignment.Center)
                .size(250.dp)
                .border(2.dp, Color.Green)
        )

        Text(
            "Đưa màn hình máy đo vào khung",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}
