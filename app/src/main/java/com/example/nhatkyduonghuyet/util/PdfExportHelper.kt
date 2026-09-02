package com.example.nhatkyduonghuyet.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardUiState

import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    suspend fun exportReportToPdf(context: Context, state: DashboardUiState): kotlin.Result<File> = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val file = File(context.cacheDir, "BaoCao_DuongHuyet_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf")
        
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 22f
                isFakeBoldText = true
            }
            val subTitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 14f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
            }
            val headerPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isFakeBoldText = true
            }
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var y = 50f
            
            // Header
            canvas.drawText("BÁO CÁO THEO DÕI ĐƯỜNG HUYẾT", 150f, y, titlePaint)
            y += 30f
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
            canvas.drawText("Ngày xuất: ${dateFormat.format(Date())}", 50f, y, textPaint)
            canvas.drawText("Thời gian: ${state.currentFilter.label}", 400f, y, textPaint)
            y += 25f
            canvas.drawLine(50f, y, 545f, y, linePaint)
            y += 30f

            // Summary Section
            canvas.drawText("1. TỔNG QUAN THỐNG KÊ", 50f, y, subTitlePaint)
            y += 25f
            
            val glucoseValues = state.entries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
                .filter { it > 0 } // PDF-03 fix: filter out non-glucose records
            val minVal = glucoseValues.minOrNull() ?: 0.0
            
            canvas.drawText("Tổng số bản ghi: ${state.entries.size}", 60f, y, textPaint)
            canvas.drawText("Đường huyết TB: %.1f mmol/L".format(Locale.US, state.avg), 250f, y, textPaint)
            y += 20f
            canvas.drawText("Thấp nhất: %.1f mmol/L".format(Locale.US, minVal), 60f, y, textPaint)
            canvas.drawText("Cao nhất: %.1f mmol/L".format(Locale.US, state.max), 250f, y, textPaint)
            y += 20f
            canvas.drawText("Ước tính HbA1c: %.1f %%".format(Locale.US, state.hba1c), 60f, y, textPaint)
            canvas.drawText("Tỷ lệ vượt ngưỡng (>10): ${state.highRate}%", 250f, y, textPaint)
            y += 30f

            // ... (keeping other sections similar for now, focusing on reliability first)
            
            // Session Stats
            val sessionGroups = state.entries.groupBy { it.session }
            canvas.drawText("Thống kê theo buổi:", 60f, y, headerPaint)
            y += 18f
            val sessions = listOf("Sáng", "Trưa", "Chiều", "Tối")
            sessions.forEach { session ->
                val vals = sessionGroups[session]?.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }?.filter { it > 0 } ?: emptyList()
                if (vals.isNotEmpty()) {
                    canvas.drawText("- $session: %.1f mmol/L (n=${vals.size})".format(Locale.US, vals.average()), 70f, y, textPaint)
                    y += 16f
                }
            }
            y += 15f

            // Medication Log Section
            if (state.medications.isNotEmpty()) {
                canvas.drawText("2. NHẬT KÝ UỐNG THUỐC", 50f, y, subTitlePaint)
                y += 25f
                canvas.drawRect(50f, y - 15f, 545f, y + 5f, Paint().apply { color = Color.rgb(240, 240, 240) })
                canvas.drawText("Tên thuốc", 55f, y, headerPaint)
                canvas.drawText("Liều dùng", 200f, y, headerPaint)
                canvas.drawText("Sáng", 350f, y, headerPaint)
                canvas.drawText("Trưa", 400f, y, headerPaint)
                canvas.drawText("Chiều", 450f, y, headerPaint)
                canvas.drawText("Tổng tháng", 500f, y, headerPaint)
                y += 20f

                state.medications.forEach { med ->
                    if (y > 780) {
                        pdfDocument.finishPage(page)
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = 50f
                        canvas.drawRect(50f, y - 15f, 545f, y + 5f, Paint().apply { color = Color.rgb(240, 240, 240) })
                        canvas.drawText("Tên thuốc", 55f, y, headerPaint)
                        canvas.drawText("Liều dùng", 200f, y, headerPaint)
                        canvas.drawText("Sáng", 350f, y, headerPaint)
                        canvas.drawText("Trưa", 400f, y, headerPaint)
                        canvas.drawText("Chiều", 450f, y, headerPaint)
                        canvas.drawText("Tổng tháng", 500f, y, headerPaint)
                        y += 20f
                    }

                    canvas.drawText(med.medication.name.take(25), 55f, y, textPaint)
                    val truncatedInst = if (med.medication.instruction.length > 25) med.medication.instruction.take(22) + "..." else med.medication.instruction
                    canvas.drawText(truncatedInst, 200f, y, textPaint)
                    
                    canvas.drawText(if (med.isTakenMorning) "X" else "-", 360f, y, textPaint)
                    canvas.drawText(if (med.isTakenNoon) "X" else "-", 410f, y, textPaint)
                    canvas.drawText(if (med.isTakenEvening) "X" else "-", 460f, y, textPaint)
                    canvas.drawText("${med.countThisMonth}", 520f, y, textPaint)

                    y += 18f
                    canvas.drawLine(50f, y - 5f, 545f, y - 5f, linePaint)
                }
                y += 15f
            }

            // AI Insights Section
            if (state.insights.isNotEmpty()) {
                canvas.drawText("3. NHẬN ĐỊNH TỪ HỆ THỐNG AI", 50f, y, subTitlePaint)
                y += 20f
                state.insights.take(3).forEach { insight ->
                    canvas.drawText("• $insight", 65f, y, textPaint)
                    y += 18f
                }
                y += 10f
            }

            // Table Header
            canvas.drawText("4. CHI TIẾT NHẬT KÝ", 50f, y, subTitlePaint)
            y += 25f
            canvas.drawRect(50f, y - 15f, 545f, y + 5f, Paint().apply { color = Color.rgb(240, 240, 240) })
            canvas.drawText("Ngày giờ", 55f, y, headerPaint)
            canvas.drawText("Buổi", 160f, y, headerPaint)
            canvas.drawText("Glucose", 220f, y, headerPaint)
            canvas.drawText("H.Áp/Tim", 300f, y, headerPaint)
            canvas.drawText("Ghi chú", 400f, y, headerPaint)
            y += 20f

            // Data Rows
            state.entries.sortedByDescending { "${it.date} ${it.time ?: ""}" }.forEach { entry ->
                val glucoseVal = entry.bgBefore ?: entry.bgAfter
                val glucoseText = if (glucoseVal != null) "%.1f".format(Locale.US, glucoseVal) else "—"
                val bp = if (entry.bpSys != null && entry.bpDia != null) "${entry.bpSys}/${entry.bpDia}" else "-"
                val hr = if (entry.heartRate != null) " (${entry.heartRate})" else ""
                val note = entry.note ?: "-"
                
                val rowHeight = calculateRowHeight(note, textPaint, 145f)
                
                if (y + rowHeight > 780) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                    canvas.drawRect(50f, y - 15f, 545f, y + 5f, Paint().apply { color = Color.rgb(240, 240, 240) })
                    canvas.drawText("Ngày giờ", 55f, y, headerPaint)
                    canvas.drawText("Buổi", 160f, y, headerPaint)
                    canvas.drawText("Glucose", 220f, y, headerPaint)
                    canvas.drawText("H.Áp/Tim", 300f, y, headerPaint)
                    canvas.drawText("Ghi chú", 400f, y, headerPaint)
                    y += 20f
                }

                val dateStr = "${entry.date} ${entry.time ?: ""}"
                canvas.drawText(dateStr, 55f, y, textPaint)
                canvas.drawText(entry.session, 160f, y, textPaint)
                canvas.drawText(glucoseText, 220f, y, textPaint)
                canvas.drawText("$bp$hr", 300f, y, textPaint)
                
                drawWrappedText(canvas, note, 400f, y, 145f, textPaint)
                
                y += rowHeight
                canvas.drawLine(50f, y - 5f, 545f, y - 5f, linePaint)
            }

            // Footer & Disclaimer
            // ...


            // Footer & Disclaimer
            y = 810f
            canvas.drawLine(50f, y - 5f, 545f, y - 5f, linePaint)
            canvas.drawText("* Lưu ý: Các nhận định AI chỉ mang tính chất tham khảo, không thay thế chẩn đoán chuyên môn của bác sĩ.", 55f, y, footerPaint)
            y += 12f
            canvas.drawText("Ứng dụng Nhật Ký Đường Huyết Pro - Hỗ trợ quản lý sức khỏe cá nhân.", 55f, y, footerPaint)

            pdfDocument.finishPage(page)

            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            kotlin.Result.success(file)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        } finally {
            pdfDocument.close()
        }
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ báo cáo PDF"))
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        var currentY = y
        val words = text.split(" ")
        var line = ""
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val testWidth = paint.measureText(testLine)
            if (testWidth > maxWidth) {
                canvas.drawText(line, x, currentY, paint)
                line = word
                currentY += paint.textSize + 2f
            } else {
                line = testLine
            }
        }
        canvas.drawText(line, x, currentY, paint)
    }

    private fun calculateRowHeight(text: String, paint: Paint, maxWidth: Float): Float {
        val words = text.split(" ")
        var lineCount = 1
        var line = ""
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val testWidth = paint.measureText(testLine)
            if (testWidth > maxWidth) {
                lineCount++
                line = word
            } else {
                line = testLine
            }
        }
        return (lineCount * (paint.textSize + 2f)).coerceAtLeast(18f)
    }
}
