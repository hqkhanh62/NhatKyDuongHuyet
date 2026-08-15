package com.example.nhatkyduonghuyet.ui.prediction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nhatkyduonghuyet.ai.PredictionOutcome
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(
    navController: NavController,
    predictor: GlucosePredictor
) {
    var fasting by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(0) }
    var outcome by remember { mutableStateOf<PredictionOutcome<Float>?>(null) }

    LaunchedEffect(fasting, type) {
        val value = fasting.replace(',', '.').toFloatOrNull()
        outcome = when {
            fasting.isBlank() -> null
            value == null -> PredictionOutcome.Failure("Nhập một số hợp lệ, ví dụ 5.6.")
            else -> predictor.predict(value, type)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dự đoán Glucose AI") },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PredictionInputCard(fasting, { fasting = it }, type, { type = it })
            Spacer(Modifier.height(24.dp))

            when (val result = outcome) {
                null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nhập chỉ số để AI bắt đầu dự đoán", color = Color.Gray)
                }
                is PredictionOutcome.Success -> PredictionResultCard(result.value)
                is PredictionOutcome.Failure -> PredictionErrorCard(result.reason)
            }
        }
    }
}

@Composable
private fun PredictionInputCard(
    fasting: String,
    onFastingChange: (String) -> Unit,
    type: Int,
    onTypeChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Thông số đầu vào", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = fasting,
                onValueChange = onFastingChange,
                label = { Text("Đường huyết lúc đói (2.0–25.0 mmol/L)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text("Thời điểm dự đoán:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(type == 0, { onTypeChange(0) }, { Text("Sau ăn trưa") }, Modifier.weight(1f))
                FilterChip(type == 1, { onTypeChange(1) }, { Text("Sau ăn chiều") }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PredictionResultCard(value: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = getRiskColor(value).copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("KẾT QUẢ DỰ ĐOÁN", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format(Locale.getDefault(), "%.1f mmol/L", value),
                style = MaterialTheme.typography.displayMedium,
                color = getRiskColor(value),
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Surface(color = getRiskColor(value), shape = RoundedCornerShape(12.dp)) {
                Text(
                    text = getInsight(value),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PredictionErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

fun getRiskColor(value: Float): Color = when {
    value < 7.0f -> Color(0xFF4CAF50)
    value < 10.0f -> Color(0xFFFF9800)
    value < 13.0f -> Color(0xFFF44336)
    else -> Color(0xFFB71C1C)
}

fun getInsight(value: Float): String = when {
    value < 7.0f -> "Ổn định 👍"
    value < 10.0f -> "Cần theo dõi"
    value < 13.0f -> "Nguy cơ cao"
    else -> "CẢNH BÁO NGUY HIỂM"
}