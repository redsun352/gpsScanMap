package com.arkeoscan.core.reports.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.arkeoscan.core.common.model.AnomalyResult
import com.arkeoscan.core.common.model.ScanSession
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Walk Scan oturumu için PDF rapor üretir: özet bilgiler (alan, çevre, grid
 * çözünürlüğü), anomali listesi (DISCLAIMER dahil) ve isteğe bağlı bir
 * heatmap/3D ekran görüntüsü (bitmap olarak gömülür; PNG export ile aynı
 * kaynaktan, bkz. ScreenshotExporter).
 */
@Singleton
class PdfReportGenerator @Inject constructor() {

    private val pageWidth = 595 // A4 @ 72dpi yaklaşık
    private val pageHeight = 842

    fun generate(
        session: ScanSession,
        anomalies: List<AnomalyResult>,
        outputFile: File,
        embeddedScreenshot: Bitmap? = null
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 14f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(60, 60, 60)
            textSize = 11f
        }
        val disclaimerPaint = Paint().apply {
            color = Color.rgb(120, 60, 0)
            textSize = 9.5f
            isFakeBoldText = true
        }

        var y = 50f
        val marginLeft = 40f

        canvas.drawText("ArkeoScan Phone — Tarama Raporu", marginLeft, y, titlePaint)
        y += 30f

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))
        canvas.drawText("Oturum: ${session.name}", marginLeft, y, headerPaint); y += 20f
        canvas.drawText(
            "Başlangıç: ${dateFormat.format(Date(session.startedAtMillis))}",
            marginLeft, y, bodyPaint
        ); y += 16f
        session.endedAtMillis?.let {
            canvas.drawText("Bitiş: ${dateFormat.format(Date(it))}", marginLeft, y, bodyPaint); y += 16f
        }
        session.areaSquareMeters?.let {
            canvas.drawText("Taranan Alan: ${"%.1f".format(it)} m²", marginLeft, y, bodyPaint); y += 16f
        }
        session.perimeterMeters?.let {
            canvas.drawText("Çevre: ${"%.1f".format(it)} m", marginLeft, y, bodyPaint); y += 16f
        }
        canvas.drawText("Grid Çözünürlüğü: ${session.gridResolutionMeters} m", marginLeft, y, bodyPaint); y += 24f

        // Ekran görüntüsü (varsa) — Heatmap/3D Surface render'ından gelen bitmap
        embeddedScreenshot?.let { bmp ->
            val targetWidth = pageWidth - 2 * marginLeft.toInt()
            val scale = targetWidth.toFloat() / bmp.width
            val targetHeight = (bmp.height * scale).toInt()

            if (y + targetHeight > pageHeight - 60f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }

            val destRect = Rect(marginLeft.toInt(), y.toInt(), marginLeft.toInt() + targetWidth, y.toInt() + targetHeight)
            canvas.drawBitmap(bmp, null, destRect, null)
            y += targetHeight + 24f
        }

        canvas.drawText("Tespit Edilen Anomaliler (${anomalies.size})", marginLeft, y, headerPaint)
        y += 22f

        if (anomalies.isEmpty()) {
            canvas.drawText("Eşik üzerinde anomali tespit edilmedi.", marginLeft, y, bodyPaint)
            y += 18f
        } else {
            for ((index, anomaly) in anomalies.withIndex()) {
                if (y > pageHeight - 100f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
                canvas.drawText(
                    "${index + 1}. ${anomaly.label} — ${"%.2f".format(anomaly.deviationSigma)}σ, " +
                        "${"%.1f".format(anomaly.areaSquareMeters)} m², ${anomaly.shape.name}, " +
                        "Güven: ${"%.0f".format(anomaly.confidenceScore * 100)}%",
                    marginLeft, y, bodyPaint
                )
                y += 16f
            }
        }

        y += 16f
        if (y > pageHeight - 80f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
        }
        canvas.drawText("ÖNEMLİ NOT:", marginLeft, y, disclaimerPaint); y += 14f
        val disclaimerLines = wrapText(AnomalyResult.DISCLAIMER, disclaimerPaint, pageWidth - 2 * marginLeft.toInt())
        for (line in disclaimerLines) {
            canvas.drawText(line, marginLeft, y, disclaimerPaint)
            y += 13f
        }

        document.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun wrapText(text: String, paint: Paint, maxWidthPx: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(candidate) <= maxWidthPx) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }
}
