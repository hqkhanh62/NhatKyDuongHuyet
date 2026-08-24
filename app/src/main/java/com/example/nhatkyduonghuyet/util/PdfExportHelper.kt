package com.example.nhatkyduonghuyet.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {
    fun exportLogEntriesToPdf(context: Context, entries: List<LogEntry>) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 12f
        }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        var y = 40f
        canvas.drawText("BÁO CÁO ĐƯỜNG HUYẾT", 200f, y, titlePaint)
        y += 40f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Ngày xuất: ${dateFormat.format(Date())}", 40f, y, textPaint)
        y += 30f

        // Headers
        canvas.drawText("Ngày giờ", 40f, y, headerPaint)
        canvas.drawText("Chỉ số (mmol/L)", 250f, y, headerPaint)
        canvas.drawText("Ghi chú", 400f, y, headerPaint)
        y += 20f
        canvas.drawLine(40f, y, 550f, y, paint)
        y += 20f

        entries.forEach { entry ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            val displayText = "${entry.date} ${entry.time ?: ""}"
            canvas.drawText(displayText, 40f, y, textPaint)
            
            val glucoseVal = entry.bgBefore ?: entry.bgAfter ?: entry.value.toDouble()
            canvas.drawText("%.1f".format(glucoseVal), 250f, y, textPaint)
            
            val noteText = "${entry.session}: ${entry.note ?: "-"}"
            canvas.drawText(noteText, 400f, y, textPaint)
            y += 20f
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "BaoCaoDuongHuyet.pdf")
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
        context.startActivity(Intent.createChooser(intent, "Chia sẻ báo cáo"))
    }
}
