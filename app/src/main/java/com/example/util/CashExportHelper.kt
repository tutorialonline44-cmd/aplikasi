package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.TransactionEntity
import com.example.model.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CashExportHelper {

    private fun getReportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Exports Cash Book (Buku Uang Kas) to a high-quality multi-page PDF document.
     */
    fun exportCashBookToPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        totalBalance: Double,
        userProfile: UserProfile,
        filterType: String = "ALL"
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val usableWidth = pageWidth - (margin * 2)

        val boldPaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val smallTextPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 0.8f
        }

        val bgCardPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        val dfDate = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        val dfTime = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        val nowStr = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).format(Date())

        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader(cv: Canvas, isFirstPage: Boolean): Float {
            var y = margin + 10f

            // Top Header Banner with requested #579F92 color
            bgCardPaint.color = Color.parseColor("#579F92")
            cv.drawRoundRect(RectF(margin, y, margin + usableWidth, y + 36f), 6f, 6f, bgCardPaint)

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            cv.drawText("SALETRENG STORE • BUKU MUTASI UANG KAS", margin + 12f, y + 23f, headerTextPaint)

            val subHeaderTextPaint = Paint().apply {
                color = Color.parseColor("#D8EBE7")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            val filterLabel = when (filterType) {
                "INCOME" -> "KAS MASUK"
                "EXPENSE" -> "KAS KELUAR"
                else -> "SEMUA MUTASI KAS"
            }
            cv.drawText(filterLabel, margin + usableWidth - 12f, y + 23f, subHeaderTextPaint)

            y += 48f

            if (isFirstPage) {
                // Info Toko & Tanggal Cetak
                cv.drawText("Nama Toko: ${userProfile.businessName}", margin, y, boldPaint)
                cv.drawText("Pemilik: ${userProfile.name}", margin, y + 14f, textPaint)
                cv.drawText("Dicetak: $nowStr", margin + usableWidth - 160f, y, smallTextPaint)
                cv.drawText("Total Mutasi: ${transactions.size} transaksi", margin + usableWidth - 160f, y + 14f, smallTextPaint)

                y += 26f
                cv.drawLine(margin, y, margin + usableWidth, y, linePaint)
                y += 12f

                // 3 Kotak Saldo Kas Toko (Saldo Total, Total Masuk, Total Keluar)
                val cardWidth = (usableWidth - 16f) / 3f
                val cardHeight = 44f

                // 1. Total Saldo Kas
                bgCardPaint.color = Color.parseColor("#E6F2F0")
                cv.drawRoundRect(RectF(margin, y, margin + cardWidth, y + cardHeight), 5f, 5f, bgCardPaint)
                val pTitle = Paint().apply { color = Color.parseColor("#386D63"); textSize = 8f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText("TOTAL SALDO KAS", margin + 8f, y + 14f, pTitle)
                val valTotal = Paint().apply { color = Color.parseColor("#264A43"); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText(Formatters.formatRupiah(totalBalance), margin + 8f, y + 33f, valTotal)

                // 2. Total Kas Masuk
                val x2 = margin + cardWidth + 8f
                bgCardPaint.color = Color.parseColor("#ECFDF5")
                cv.drawRoundRect(RectF(x2, y, x2 + cardWidth, y + cardHeight), 5f, 5f, bgCardPaint)
                val pGreen = Paint().apply { color = Color.parseColor("#047857"); textSize = 8f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText("TOTAL KAS MASUK (+)", x2 + 8f, y + 14f, pGreen)
                val valGreen = Paint().apply { color = Color.parseColor("#065F46"); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText(Formatters.formatRupiah(totalIncome), x2 + 8f, y + 33f, valGreen)

                // 3. Total Kas Keluar
                val x3 = x2 + cardWidth + 8f
                bgCardPaint.color = Color.parseColor("#FEF2F2")
                cv.drawRoundRect(RectF(x3, y, x3 + cardWidth, y + cardHeight), 5f, 5f, bgCardPaint)
                val pRed = Paint().apply { color = Color.parseColor("#B91C1C"); textSize = 8f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText("TOTAL KAS KELUAR (-)", x3 + 8f, y + 14f, pRed)
                val valRed = Paint().apply { color = Color.parseColor("#991B1B"); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText(Formatters.formatRupiah(totalExpense), x3 + 8f, y + 33f, valRed)

                y += cardHeight + 14f
            }

            return y
        }

        var currentY = drawHeader(canvas, isFirstPage = true)

        // Tabel Mutasi Kas
        fun drawTableHeader(cv: Canvas, y: Float): Float {
            bgCardPaint.color = Color.parseColor("#F3F4F6")
            cv.drawRoundRect(RectF(margin, y, margin + usableWidth, y + 20f), 3f, 3f, bgCardPaint)

            val thPaint = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            cv.drawText("NO", margin + 6f, y + 13f, thPaint)
            cv.drawText("TANGGAL & JAM", margin + 28f, y + 13f, thPaint)
            cv.drawText("KETERANGAN MUTASI KAS", margin + 130f, y + 13f, thPaint)

            val thRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }
            cv.drawText("KAS MASUK (+)", margin + 395f, y + 13f, thRight)
            cv.drawText("KAS KELUAR (-)", margin + usableWidth - 6f, y + 13f, thRight)

            return y + 24f
        }

        currentY = drawTableHeader(canvas, currentY)

        val sortedTransactions = transactions.sortedByDescending { it.timestamp }
        val rowHeight = 24f
        val greenAmtPaint = Paint().apply {
            color = Color.parseColor("#059669")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val redAmtPaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val zebraPaint = Paint().apply {
            color = Color.parseColor("#FAFAFA")
            style = Paint.Style.FILL
        }

        sortedTransactions.forEachIndexed { index, tx ->
            if (currentY + rowHeight > pageHeight - margin - 20f) {
                drawFooter(canvas, currentPageNumber, margin, usableWidth, pageHeight, smallTextPaint)
                document.finishPage(page)

                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                currentY = drawHeader(canvas, isFirstPage = false)
                currentY = drawTableHeader(canvas, currentY)
            }

            if (index % 2 == 1) {
                canvas.drawRect(margin, currentY - 4f, margin + usableWidth, currentY + rowHeight - 6f, zebraPaint)
            }

            // No
            canvas.drawText("${index + 1}", margin + 6f, currentY + 11f, smallTextPaint)

            // Tanggal & Jam
            val dateFormatted = dfDate.format(Date(tx.timestamp))
            val timeFormatted = dfTime.format(Date(tx.timestamp))
            canvas.drawText("$dateFormatted $timeFormatted", margin + 28f, currentY + 11f, textPaint)

            // Keterangan Mutasi Kas
            val cleanTitle = if (tx.title.length > 32) tx.title.take(30) + ".." else tx.title
            canvas.drawText(cleanTitle, margin + 130f, currentY + 11f, boldPaint)

            // Nominal Masuk / Keluar
            if (tx.type == "INCOME") {
                canvas.drawText(Formatters.formatRupiah(tx.amount), margin + 395f, currentY + 11f, greenAmtPaint)
                canvas.drawText("-", margin + usableWidth - 30f, currentY + 11f, smallTextPaint)
            } else {
                canvas.drawText("-", margin + 350f, currentY + 11f, smallTextPaint)
                canvas.drawText(Formatters.formatRupiah(tx.amount), margin + usableWidth - 6f, currentY + 11f, redAmtPaint)
            }

            // Separator line
            canvas.drawLine(margin, currentY + rowHeight - 6f, margin + usableWidth, currentY + rowHeight - 6f, linePaint)
            currentY += rowHeight
        }

        // Draw final footer
        drawFooter(canvas, currentPageNumber, margin, usableWidth, pageHeight, smallTextPaint)
        document.finishPage(page)

        val timeTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "Buku_Kas_Saletreng_${timeTag}.pdf"
        val outFile = File(getReportsDir(context), fileName)

        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return outFile
    }

    private fun drawFooter(
        cv: Canvas,
        pageNum: Int,
        margin: Float,
        usableWidth: Float,
        pageHeight: Int,
        paint: Paint
    ) {
        val y = pageHeight - margin + 6f
        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 0.5f
        }
        cv.drawLine(margin, y - 8f, margin + usableWidth, y - 8f, linePaint)
        cv.drawText("Dicetak oleh Saletreng Store • Buku Uang Kas Resmi", margin, y + 2f, paint)

        val pagePaint = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
        cv.drawText("Hal $pageNum", margin + usableWidth, y + 2f, pagePaint)
    }

    /**
     * Exports Cash Book (Buku Uang Kas) to a structured CSV file compatible with Microsoft Excel & Google Sheets.
     */
    fun exportCashBookToExcel(
        context: Context,
        transactions: List<TransactionEntity>,
        totalBalance: Double,
        userProfile: UserProfile,
        filterType: String = "ALL"
    ): File {
        val timeTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "Buku_Kas_Saletreng_${timeTag}.csv"
        val outFile = File(getReportsDir(context), fileName)

        val dfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dfTime = SimpleDateFormat("HH:mm:ss", Locale.US)
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netCashFlow = totalIncome - totalExpense

        val sb = StringBuilder()
        // UTF-8 BOM for Microsoft Excel auto-detect UTF-8
        sb.append('\uFEFF')

        fun esc(value: String): String {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }

        // Header Dokumen
        sb.append(esc("SALETRENG STORE - BUKU MUTASI UANG KAS")).append("\n")
        sb.append(esc("Nama Toko")).append(",").append(esc(userProfile.businessName)).append("\n")
        sb.append(esc("Pemilik")).append(",").append(esc(userProfile.name)).append("\n")
        sb.append(esc("Tanggal Unduh")).append(",").append(esc(nowStr)).append("\n")
        sb.append(esc("Total Mutasi")).append(",").append(transactions.size).append("\n")
        sb.append("\n")

        // Ringkasan Saldo Kas
        sb.append(esc("RINGKASAN UANG KAS")).append("\n")
        sb.append(esc("Total Saldo Kas")).append(",").append(String.format(Locale.US, "%.0f", totalBalance)).append("\n")
        sb.append(esc("Total Kas Masuk (+)")).append(",").append(String.format(Locale.US, "%.0f", totalIncome)).append("\n")
        sb.append(esc("Total Kas Keluar (-)")).append(",").append(String.format(Locale.US, "%.0f", totalExpense)).append("\n")
        sb.append(esc("Arus Kas Bersih")).append(",").append(String.format(Locale.US, "%.0f", netCashFlow)).append("\n")
        sb.append("\n")

        // Tabel Mutasi Kas
        sb.append(esc("TABEL MUTASI BUKU KAS")).append("\n")
        sb.append(esc("No")).append(",")
            .append(esc("Tanggal")).append(",")
            .append(esc("Waktu")).append(",")
            .append(esc("Keterangan Mutasi Kas")).append(",")
            .append(esc("Tipe")).append(",")
            .append(esc("Kas Masuk (Rp)")).append(",")
            .append(esc("Kas Keluar (Rp)")).append(",")
            .append(esc("Catatan")).append("\n")

        val sortedTransactions = transactions.sortedByDescending { it.timestamp }
        sortedTransactions.forEachIndexed { index, tx ->
            val dateStr = dfDate.format(Date(tx.timestamp))
            val timeStr = dfTime.format(Date(tx.timestamp))
            val isIncome = tx.type == "INCOME"

            val masukAmt = if (isIncome) String.format(Locale.US, "%.0f", tx.amount) else "0"
            val keluarAmt = if (!isIncome) String.format(Locale.US, "%.0f", tx.amount) else "0"

            sb.append(index + 1).append(",")
                .append(esc(dateStr)).append(",")
                .append(esc(timeStr)).append(",")
                .append(esc(tx.title)).append(",")
                .append(esc(if (isIncome) "Kas Masuk" else "Kas Keluar")).append(",")
                .append(masukAmt).append(",")
                .append(keluarAmt).append(",")
                .append(esc(tx.note)).append("\n")
        }

        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }
}
