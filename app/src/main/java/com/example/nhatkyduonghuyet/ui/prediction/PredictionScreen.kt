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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

import androidx.compose.ui.res.stringResource
import com.example.nhatkyduonghuyet.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(
    navController: NavController,
    predictor: GlucosePredictor
) {
    var fasting by remember { mutableStateOf("") }
    var type by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<PredictionOutcome<Float>?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    
    val invalidNumberMsg = stringResource(R.string.invalid_number_error)

    fun performPrediction() {
        debounceJob?.cancel()
        val value = fasting.replace(',', '.').toFloatOrNull()

        if (value == null) {
            outcome = if (fasting.isBlank()) null
            else PredictionOutcome.Failure(invalidNumberMsg)
            return
        }

        debounceJob = scope.launch {
            isLoading = true
            try {
                delay(500)
                outcome = predictor.predict(value, type)
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prediction_title)) },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
            PredictionInputCard(
                fasting = fasting,
                onFastingChange = {
                    fasting = it
                    performPrediction()
                },
                type = type,
                onTypeChange = {
                    type = it
                    performPrediction()
                }
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { performPrediction() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && fasting.isNotBlank()
            ) {
                Text(if (isLoading) stringResource(R.string.analyzing) else stringResource(R.string.predict_now))
            }

            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.ai_is_analyzing), color = Color.Gray)
                }
                outcome == null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.input_instruction), color = Color.Gray)
                }
                outcome is PredictionOutcome.Success -> PredictionResultCard(
                    (outcome as PredictionOutcome.Success).value
                )
                outcome is PredictionOutcome.Failure -> PredictionErrorCard(
                    (outcome as PredictionOutcome.Failure).reason
                )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.input_parameters),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = fasting,
                onValueChange = onFastingChange,
                label = { Text(stringResource(R.string.fasting_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.prediction_time), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = type == 0,
                    onClick = { onTypeChange(0) },
                    label = { Text(stringResource(R.string.after_lunch)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = type == 1,
                    onClick = { onTypeChange(1) },
                    label = { Text(stringResource(R.string.after_dinner)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PredictionResultCard(value: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = getRiskColor(value).copy(alpha = 0.1f)
        )
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.prediction_result),
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format(Locale.getDefault(), "%.1f mmol/L", value),
                style = MaterialTheme.typography.displayMedium,
                color = getRiskColor(value),
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                color = getRiskColor(value),
                shape = RoundedCornerShape(12.dp)
            ) {
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
        Text(
            message,
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun getRiskColor(value: Float): Color = when {
    value < 4.0f -> Color(0xFFD32F2F)
    value < 7.0f -> Color(0xFF4CAF50)
    value < 10.0f -> Color(0xFFFF9800)
    value < 13.0f -> Color(0xFFF44336)
    else -> Color(0xFFB71C1C)
}

@Composable
fun getInsight(value: Float): String = when {
    value < 4.0f -> stringResource(R.string.status_low)
    value < 7.0f -> stringResource(R.string.status_stable)
    value < 10.0f -> stringResource(R.string.status_monitor)
    value < 13.0f -> stringResource(R.string.status_high_risk)
    else -> stringResource(R.string.status_danger)
}
