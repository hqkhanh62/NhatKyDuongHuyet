package com.example.nhatkyduonghuyet.ui.scanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nhatkyduonghuyet.ai.PredictionResult
import com.example.nhatkyduonghuyet.domain.scanner.AlertLevel
import com.example.nhatkyduonghuyet.domain.scanner.AutoImportPipeline
import com.example.nhatkyduonghuyet.domain.scanner.FieldSource
import com.example.nhatkyduonghuyet.domain.scanner.GlucoseSession
import com.example.nhatkyduonghuyet.domain.scanner.ReadingSlot
import com.example.nhatkyduonghuyet.domain.scanner.RiskAlert
import com.example.nhatkyduonghuyet.domain.scanner.ScanInsight

private val BANNER_BACKGROUND = Color(0xF2101518)
private val BANNER_TEXT = Color(0xFFE8F1EA)
private val LEVEL_INFO = Color(0xFF4FC3F7)
private val LEVEL_WARNING = Color(0xFFFFB300)
private val LEVEL_CRITICAL = Color(0xFFFF5252)
private val SAFE_GREEN = Color(0xFF4CAF50)

/**
 * Banner kết quả tức thì ngay dưới camera: cho phép kiểm tra chỉ số, buổi, ô
 * trước/sau ăn và ngày giờ mà AI tự điền *trước khi* xác nhận lưu.
 */
@Composable
fun ScanReviewBanner(
    draft: AutoImportPipeline.ScanDraft,
    insight: ScanInsight?,
    forecast: PredictionResult?,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onRescan: () -> Unit,
    onAdjustValue: (Float) -> Unit,
    onSelectSession: (GlucoseSession) -> Unit,
    onSelectSlot: (ReadingSlot) -> Unit,
    onUseSystemTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScanBannerCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SAFE_GREEN, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "AI ĐỌC ĐƯỢC - KIỂM TRA TRƯỚC KHI LƯU",
                style = MaterialTheme.typography.labelMedium,
                color = SAFE_GREEN,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            SourceChip("${draft.valueSource} • ${ConfidenceLabel(draft.confidence)}")
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAdjustValue(-0.1f) }, enabled = !isSaving) {
                Icon(Icons.Default.Remove, contentDescription = "Giảm 0.1", tint = BANNER_TEXT)
            }
            Text(
                text = draft.valueText,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = BANNER_TEXT
            )
            Text(" mmol/L", color = BANNER_TEXT.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onAdjustValue(0.1f) }, enabled = !isSaving) {
                Icon(Icons.Default.Add, contentDescription = "Tăng 0.1", tint = BANNER_TEXT)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Buổi tự phân loại theo giờ AI quét được - người dùng vẫn sửa được.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlucoseSession.values().forEach { session ->
                BannerChip(
                    text = session.label,
                    selected = draft.session == session,
                    enabled = !isSaving,
                    onClick = { onSelectSession(session) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReadingSlot.values().forEach { slot ->
                BannerChip(
                    text = slot.label,
                    selected = draft.slot == slot,
                    enabled = !isSaving,
                    onClick = { onSelectSlot(slot) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                AutoFilledRow(
                    label = "Ngày",
                    value = draft.dateText,
                    source = draft.dateSource
                )
                Spacer(Modifier.height(2.dp))
                AutoFilledRow(
                    label = "Giờ",
                    value = draft.time,
                    source = draft.timeSource
                )
            }
            if (draft.timeSource == FieldSource.METER && !isSaving) {
                TextButton(onClick = onUseSystemTime) {
                    Text("Dùng giờ điện thoại", fontSize = 11.sp, color = LEVEL_INFO)
                }
            }
        }

        // Dong mm-dd chu nho o tren cung man hinh la de bi nham nhat: hai cach doc
        // deu hop le ve lich thi AI khong the tu ket luan, nen bao ro trong banner.
        if (draft.dateAmbiguous) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ngày máy đo viết dạng MM-DD nên có thể bị đảo với DD/MM - " +
                    "kiểm lại trước khi lưu",
                color = LEVEL_WARNING,
                fontSize = 11.sp
            )
        }

        if (draft.confidence < 0.6f) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Độ tin cậy OCR thấp - nên đối chiếu lại với màn hình máy đo.",
                color = LEVEL_WARNING,
                fontSize = 11.sp
            )
        }

        insight?.let {
            Spacer(Modifier.height(8.dp))
            ScanInsightSummary(it, forecast)
        }

        OcrTextDisclosure(draft.ocrText)

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SAFE_GREEN, contentColor = Color(0xFF06240D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF06240D))
                    Spacer(Modifier.width(8.dp))
                    Text("ĐANG LƯU", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("XÁC NHẬN & LƯU", fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(
                onClick = onRescan,
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("QUÉT LẠI", color = BANNER_TEXT)
            }
        }
    }
}

/** Banner sau khi lưu: kết quả vòng AI (dự báo, HbA1c, cảnh báo) + hoàn tác. */
@Composable
fun ScanSavedBanner(
    saved: ScanPhase.Saved,
    retrainRequested: Boolean,
    onUndo: () -> Unit,
    onScanAgain: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScanBannerCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(SAFE_GREEN, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF06240D), modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "ĐÃ LƯU VÀO NHẬT KÝ ${saved.draft.dateText}",
                style = MaterialTheme.typography.labelMedium,
                color = SAFE_GREEN,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            SourceChip("${saved.draft.session.label} • ${saved.draft.slot.label}")
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = saved.draft.valueText,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = BANNER_TEXT
            )
            Text(" mmol/L • ${saved.draft.time}", color = BANNER_TEXT.copy(alpha = 0.75f), fontSize = 13.sp)
        }

        Spacer(Modifier.height(8.dp))
        ScanInsightSummary(saved.insight, saved.forecast)

        if (retrainRequested) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Đủ dữ liệu mới để huấn lại mô hình AI - App sẽ tự hiệu chỉnh.",
                color = LEVEL_INFO,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onUndo, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("HOÀN TÁC", color = LEVEL_WARNING, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onScanAgain, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("QUÉT TIẾP", color = BANNER_TEXT, fontSize = 12.sp)
            }
            Button(onClick = onDone, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SAFE_GREEN, contentColor = Color(0xFF06240D)), shape = RoundedCornerShape(12.dp)) {
                Text("XONG", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Tóm tắt vòng AI: HbA1c tính lại, TIR, dự báo và danh sách cảnh báo. */
@Composable
fun ScanInsightSummary(
    insight: ScanInsight,
    forecast: PredictionResult?
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Metric(
            label = "HbA1c ước tính",
            value = if (insight.hba1c.isAvailable) {
                "${String.format(java.util.Locale.US, "%.1f", insight.hba1c.percent)}%"
            } else {
                "--"
            },
            note = insight.hba1cDeltaText?.let { "($it)" }
        )
        Metric(
            label = "Trong mục tiêu",
            value = insight.timeInRangePercent?.let { "$it%" } ?: "--",
            note = "${insight.sampleCount} chỉ số"
        )
        Metric(
            label = "Dự báo 6h",
            value = forecast?.let { "${AutoImportPipeline.formatMmol(it.next)}" } ?: "--",
            note = forecast?.let { if (it.trend >= 0f) "📈 +${AutoImportPipeline.formatMmol(it.trend)}" else "📉 ${AutoImportPipeline.formatMmol(it.trend)}" }
        )
    }

    Spacer(Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            insight.headline,
            color = colorFor(insight.alerts.map { it.level }),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        insight.alerts.forEach { alert -> RiskAlertRow(alert) }
    }
}

@Composable
private fun RiskAlertRow(alert: RiskAlert) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .background(colorFor(alert.level), shape = CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(alert.message, color = BANNER_TEXT.copy(alpha = 0.92f), fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AutoFilledRow(label: String, value: String, source: FieldSource) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = BANNER_TEXT.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(value, color = BANNER_TEXT, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
            " (${source.label})",
            color = if (source == FieldSource.METER) SAFE_GREEN else LEVEL_INFO,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun RowScope.Metric(label: String, value: String, note: String?) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, color = BANNER_TEXT.copy(alpha = 0.6f), fontSize = 10.sp)
        Text(value, color = BANNER_TEXT, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        note?.let { Text(it, color = BANNER_TEXT.copy(alpha = 0.55f), fontSize = 10.sp) }
    }
}

@Composable
private fun BannerChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .then(
                if (selected) {
                    Modifier.background(SAFE_GREEN.copy(alpha = 0.9f), shape).border(1.dp, SAFE_GREEN, shape)
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.08f), shape).border(1.dp, Color.White.copy(alpha = 0.25f), shape)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF06240D) else BANNER_TEXT.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SourceChip(text: String) {
    BannerChip(text = text, selected = false, enabled = false, onClick = {})
}

@Composable
private fun OcrTextDisclosure(ocrText: String) {
    if (ocrText.isBlank()) return
    var expanded by remember { mutableStateOf(false) }
    Spacer(Modifier.height(6.dp))
    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Ẩn văn bản OCR" else "Xem văn bản OCR", fontSize = 11.sp, color = LEVEL_INFO)
    }
    if (expanded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(ocrText, color = BANNER_TEXT.copy(alpha = 0.8f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ScanBannerCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BANNER_BACKGROUND,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

private fun colorFor(levels: List<AlertLevel>): Color = when {
    levels.contains(AlertLevel.CRITICAL) -> LEVEL_CRITICAL
    levels.contains(AlertLevel.WARNING) -> LEVEL_WARNING
    else -> LEVEL_INFO
}

private fun colorFor(level: AlertLevel): Color = when (level) {
    AlertLevel.CRITICAL -> LEVEL_CRITICAL
    AlertLevel.WARNING -> LEVEL_WARNING
    AlertLevel.INFO -> LEVEL_INFO
}

private fun ConfidenceLabel(confidence: Float): String = when {
    confidence >= 0.8f -> "tin cậy cao"
    confidence >= 0.6f -> "tin cậy khá"
    else -> "cần kiểm tra"
}
