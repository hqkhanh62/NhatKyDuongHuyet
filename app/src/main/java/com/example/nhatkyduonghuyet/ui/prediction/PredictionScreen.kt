package com.example.nhatkyduonghuyet.ui.prediction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.ml.GlucosePredictor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(
    navController: NavController,
    predictor: GlucosePredictor
) {
    var fasting by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(0) } // 0: Trưa, 1: Chiều
    var result by remember { mutableStateOf<Float?>(null) }

    // Real-time Prediction logic
    LaunchedEffect(fasting, type) {
        val f = fasting.replace(',', '.').toFloatOrNull()
        if (f != null) {
            result = predictor.predict(f, type)
        } else {
            result = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dự đoán Glucose AI") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Thông số đầu vào", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = fasting,
                        onValueChange = { fasting = it },
                        label = { Text("Đường huyết lúc đói (mmol/L)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Thời điểm dự đoán:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = type == 0,
                            onClick = { type = 0 },
                            label = { Text("Sau ăn Trưa") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = type == 1,
                            onClick = { type = 1 },
                            label = { Text("Sau ăn Chiều") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            result?.let { valResult ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = getRiskColor(valResult).copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("KẾT QUẢ DỰ ĐOÁN", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "%.1f mmol/L".format(valResult),
                            style = MaterialTheme.typography.displayMedium,
                            color = getRiskColor(valResult),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = getRiskColor(valResult),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = getInsight(valResult),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nhập chỉ số để AI bắt đầu dự đoán", color = Color.Gray)
                }
            }
        }
    }
}

fun getRiskColor(value: Float): Color {
    return when {
        value < 7.0f -> Color(0xFF4CAF50)      // Normal
        value < 10.0f -> Color(0xFFFF9800)     // Warning
        value < 13.0f -> Color(0xFFF44336)     // High
        else -> Color(0xFFB71C1C)           // Critical
    }
}

fun getInsight(value: Float): String {
    return when {
        value < 7.0f -> "Ổn định 👍"
        value < 10.0f -> "Cần theo dõi"
        value < 13.0f -> "Nguy cơ cao"
        else -> "CẢNH BÁO NGUY HIỂM"
    }
}
