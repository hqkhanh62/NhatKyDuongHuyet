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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.graphics.Typeface

object PdfExportHelper {

    fun exportReportToPdf(context: Context, state: DashboardUiState) {
        val pdfDocument = PdfDocument()
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
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Ngày xuất: ${dateFormat.format(Date())}", 50f, y, textPaint)
        canvas.drawText("Thời gian: ${state.currentFilter.label}", 400f, y, textPaint)
        y += 25f
        canvas.drawLine(50f, y, 545f, y, linePaint)
        y += 30f

        // Summary Section
        canvas.drawText("1. TỔNG QUAN THỐNG KÊ", 50f, y, subTitlePaint)
        y += 25f
        
        val glucoseValues = state.entries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
        val minVal = glucoseValues.minOrNull() ?: 0.0
        
        canvas.drawText("Tổng số bản ghi: ${state.entries.size}", 60f, y, textPaint)
        canvas.drawText("Đường huyết TB: %.1f mmol/L".format(state.avg), 250f, y, textPaint)
        y += 20f
        canvas.drawText("Thấp nhất: %.1f mmol/L".format(minVal), 60f, y, textPaint)
        canvas.drawText("Cao nhất: %.1f mmol/L".format(state.max), 250f, y, textPaint)
        y += 20f
        canvas.drawText("Ước tính HbA1c: %.1f %%".format(state.hba1c), 60f, y, textPaint)
        canvas.drawText("Tỷ lệ vượt ngưỡng (>10): ${state.highRate}%", 250f, y, textPaint)
        y += 30f

        // Session Stats
        val sessionGroups = state.entries.groupBy { it.session }
        canvas.drawText("Thống kê theo buổi:", 60f, y, headerPaint)
        y += 18f
        val sessions = listOf("Sáng", "Trưa", "Chiều", "Tối")
        sessions.forEach { session ->
            val vals = sessionGroups[session]?.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) } ?: emptyList()
            if (vals.isNotEmpty()) {
                canvas.drawText("- $session: %.1f mmol/L (n=${vals.size})".format(vals.average()), 70f, y, textPaint)
                y += 16f
            }
        }
        y += 15f

        // AI Insights Section
        if (state.insights.isNotEmpty()) {
            canvas.drawText("2. NHẬN ĐỊNH TỪ HỆ THỐNG AI", 50f, y, subTitlePaint)
            y += 20f
            state.insights.take(3).forEach { insight ->
                canvas.drawText("• $insight", 65f, y, textPaint)
                y += 18f
            }
            y += 10f
        }

        // Table Header
        canvas.drawText("3. CHI TIẾT NHẬT KÝ", 50f, y, subTitlePaint)
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
            if (y > 780) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
                // Redraw table headers on new page
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
            
            val glucose = entry.bgBefore ?: entry.bgAfter ?: entry.value.toDouble()
            canvas.drawText("%.1f".format(glucose), 220f, y, textPaint)
            
            val bp = if (entry.bpSys != null && entry.bpDia != null) "${entry.bpSys}/${entry.bpDia}" else "-"
            val hr = if (entry.heartRate != null) " (${entry.heartRate})" else ""
            canvas.drawText("$bp$hr", 300f, y, textPaint)
            
            val note = entry.note ?: "-"
            val truncatedNote = if (note.length > 25) note.take(22) + "..." else note
            canvas.drawText(truncatedNote, 400f, y, textPaint)
            
            y += 18f
            canvas.drawLine(50f, y - 5f, 545f, y - 5f, linePaint)
        }

        // Footer & Disclaimer
        y = 810f
        canvas.drawLine(50f, y - 5f, 545f, y - 5f, linePaint)
        canvas.drawText("* Lưu ý: Các nhận định AI chỉ mang tính chất tham khảo, không thay thế chẩn đoán chuyên môn của bác sĩ.", 55f, y, footerPaint)
        y += 12f
        canvas.drawText("Ứng dụng Nhật Ký Đường Huyết Pro - Hỗ trợ quản lý sức khỏe cá nhân.", 55f, y, footerPaint)

        pdfDocument.finishPage(page)

        val fileName = "BaoCao_DuongHuyet_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        val file = File(context.cacheDir, fileName)
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()

        shareFile(context, file)
    }

    private fun shareFile(context: Context, file: File) {
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
}
