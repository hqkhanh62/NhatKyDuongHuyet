package com.example.nhatkyduonghuyet.util

import android.content.Context
import android.net.Uri
import com.example.nhatkyduonghuyet.data.local.entity.LogEntry
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.text.SimpleDateFormat
import java.util.*

object PdfExportHelper {

    fun exportToPdf(
        context: Context,
        uri: Uri,
        entries: List<LogEntry>,
        patientName: String = "Bệnh nhân"
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                document.add(
                    Paragraph("BÁO CÁO ĐƯỜNG HUYẾT")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(20f)
                        .setBold()
                )

                document.add(Paragraph("Bệnh nhân: $patientName").setFontSize(12f))
                document.add(
                    Paragraph("Ngày xuất: ${dateFormat.format(Date())}")
                        .setFontSize(12f)
                        .setMarginBottom(20f)
                )

                val values = entries.flatMap { listOfNotNull(it.bgBefore, it.bgAfter) }
                if (values.isNotEmpty()) {
                    val avg = values.average()
                    val max = values.maxOrNull() ?: 0.0
                    val min = values.minOrNull() ?: 0.0
                    val highCount = values.count { it > 10.0 }
                    val lowCount = values.count { it < 3.9 }

                    document.add(Paragraph("THỐNG KÊ").setBold().setFontSize(14f))
                    document.add(Paragraph("Trung bình: %.1f mmol/L".format(avg)))
                    document.add(Paragraph("Cao nhất: %.1f mmol/L".format(max)))
                    document.add(Paragraph("Thấp nhất: %.1f mmol/L".format(min)))
                    document.add(Paragraph("Vượt ngưỡng (>10): $highCount lần"))
                    document.add(Paragraph("Thấp ngưỡng (<3.9): $lowCount lần"))
                    document.add(Paragraph("Tổng số bản ghi: ${entries.size}").setMarginBottom(20f))
                }

                document.add(Paragraph("CHI TIẾT CÁC LẦN ĐO").setBold().setFontSize(14f))

                val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1.5f, 1.5f, 2f, 2f, 3f)))
                    .useAllAvailableWidth()

                listOf("Ngày", "Buổi", "Giờ", "Trước", "Sau", "Ghi chú").forEach {
                    table.addHeaderCell(Cell().add(Paragraph(it).setBold()))
                }

                entries.sortedBy { it.date }.forEach { entry ->
                    table.addCell(entry.date)
                    table.addCell(entry.session)
                    table.addCell(entry.time ?: "--")
                    table.addCell(entry.bgBefore?.toString() ?: "--")
                    table.addCell(entry.bgAfter?.toString() ?: "--")
                    table.addCell(entry.note ?: "")
                }

                document.add(table)

                document.add(
                    Paragraph("\nBáo cáo từ ứng dụng Nhật Ký Đường Huyết")
                        .setFontSize(9f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(20f)
                )

                document.close()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
