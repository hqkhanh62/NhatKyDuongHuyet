package com.example.nhatkyduonghuyet.ui.scanner.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.domain.GlucoseRiskLevel
import com.example.nhatkyduonghuyet.scan.Hba1cEstimator
import com.example.nhatkyduonghuyet.scan.ScanDateSource
import com.example.nhatkyduonghuyet.scan.ScanImportDraft
import com.example.nhatkyduonghuyet.scan.ScanSaveResult
import com.example.nhatkyduonghuyet.scan.ScanTimeSource
import java.util.Locale

/**
 * Instant result banner shown right below the camera for pre-confirm review
 * (Pro AI Auto Import Pipeline).
 *
 * REVIEW: value + session + meter/system date-time chips + risk + provisional
 * HbA1c + [Xác nhận & Lưu] / [Quét lại].
 *
 * SAVED: success summary + LSTM forecast + warnings + [Quét tiếp]/[Xem nhật ký].
 */
@Composable
fun ScanReviewBanner(
    draft: ScanImportDraft,
    isSaving: Boolean,
    saveError: String?,
    onConfirm: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColor = riskColor(draft.risk.level)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Kết quả quét AI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "${(draft.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", draft.value),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = riskColor,
                        fontSize = 44.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "mmol/L",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                SessionChip(draft.session)
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateTimeChip(
                    label = draft.date,
                    fromMeter = draft.dateSource == ScanDateSource.METER
                )
                DateTimeChip(
                    label = draft.time,
                    fromMeter = draft.timeSource == ScanTimeSource.METER
                )
            }

            Spacer(Modifier.height(10.dp))

            RiskRow(message = draft.risk.message, color = riskColor)

            Spacer(Modifier.height(6.dp))

            Text(
                text = "HbA1c ước tính: ${Hba1cEstimator.format(draft.hba1cProvisional)} (tạm tính)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            saveError?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRescan,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Quét lại")
                }
                Button(
                    onClick = onConfirm,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (draft.risk.isDanger) Color(0xFFD32F2F)
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Đang lưu…")
                    } else {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Xác nhận & Lưu")
                    }
                }
            }
        }
    }
}

@Composable
fun ScanSavedBanner(
    draft: ScanImportDraft,
    saveResult: ScanSaveResult?,
    forecast: PredictionResult?,
    forecastMessage: String?,
    retrainReady: Boolean,
    onRescan: () -> Unit,
    onViewDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDuplicate = saveResult is ScanSaveResult.Duplicate
    val riskColor = riskColor(draft.risk.level)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDuplicate) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDuplicate) Color(0xFFF57C00) else Color(0xFF2E7D32),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isDuplicate) "Đã tồn tại — không lưu trùng"
                    else "Đã lưu vào nhật ký",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${String.format(Locale.US, "%.1f", draft.value)} mmol/L • " +
                    "Buổi ${draft.session.lowercase()} • ${draft.date} ${draft.time}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            RiskRow(message = draft.risk.message, color = riskColor)

            Spacer(Modifier.height(8.dp))

            // Real-time AI Loop: forecast refreshed from the just-saved history.
            when {
                forecast != null -> {
                    val trendIcon = when {
                        forecast.trend > 0.3f -> "📈"
                        forecast.trend < -0.3f -> "📉"
                        else -> "➡️"
                    }
                    Text(
                        text = "$trendIcon AI dự báo lần đo tới: " +
                            "${String.format(Locale.US, "%.1f", forecast.next)} mmol/L " +
                            "(${forecast.risk})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                forecastMessage != null -> {
                    Text(
                        text = "🔮 $forecastMessage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (retrainReady) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "🧠 Đủ dữ liệu mới — AI đã sẵn sàng nâng cấp trên Dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Quét tiếp")
                }
                Button(onClick = onViewDiary, modifier = Modifier.weight(1.2f)) {
                    Text("Xem nhật ký")
                }
            }
        }
    }
}

@Composable
private fun SessionChip(session: String) {
    val color = when (session) {
        "Sáng" -> Color(0xFF1976D2)
        "Trưa" -> Color(0xFFF57C00)
        "Chiều" -> Color(0xFF7B1FA2)
        else -> Color(0xFF388E3C)
    }
    AssistChip(
        onClick = {},
        label = { Text("Buổi $session", fontWeight = FontWeight.Bold) },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color,
            leadingIconContentColor = color
        )
    )
}

@Composable
private fun DateTimeChip(label: String, fromMeter: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        trailingIcon = {
            Text(
                text = if (fromMeter) "Máy đo" else "Hệ thống",
                style = MaterialTheme.typography.labelSmall,
                color = if (fromMeter) Color(0xFF2E7D32)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    )
}

@Composable
private fun RiskRow(message: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (color == Color(0xFF2E7D32)) Icons.Default.CheckCircle
            else Icons.Default.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private fun riskColor(level: GlucoseRiskLevel): Color = try {
    Color(android.graphics.Color.parseColor(level.colorCode))
} catch (_: Exception) {
    Color(0xFF4CAF50)
}
