package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.ProductEntity
import com.example.data.TransactionEntity
import com.example.model.MonthlySummary
import com.example.model.ProductRegistry
import com.example.model.UserProfile
import com.example.model.WalletType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportHelper {

    private fun getReportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Generates a professional multi-page bookkeeping PDF document.
     */
    fun exportToPdf(
        context: Context,
        summary: MonthlySummary,
        transactions: List<TransactionEntity>,
        customProducts: List<ProductEntity>,
        userProfile: UserProfile
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val usableWidth = pageWidth - (margin * 2)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#386D63") // Sage Teal Dark
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val smallTextPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val bgCardPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        val dfDate = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        val dfTime = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        val nowStr = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).format(Date())

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader(cv: Canvas, isFirstPage: Boolean): Float {
            var y = margin + 10f

            // Top Header Bar
            bgCardPaint.color = Color.parseColor("#579F92") // Requested #579F92
            cv.drawRoundRect(RectF(margin, y, margin + usableWidth, y + 36f), 6f, 6f, bgCardPaint)

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            cv.drawText("SALETRENG STORE • LAPORAN PEMBUKUAN KEUANGAN", margin + 12f, y + 23f, headerTextPaint)

            val subHeaderTextPaint = Paint().apply {
                color = Color.parseColor("#D8EBE7")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            cv.drawText("${summary.monthName.uppercase()} ${summary.year}", margin + usableWidth - 12f, y + 23f, subHeaderTextPaint)

            y += 48f

            if (isFirstPage) {
                // Info Usaha & Periode
                cv.drawText("Nama Usaha: ${userProfile.businessName}", margin, y, boldPaint)
                cv.drawText("Pemilik: ${userProfile.name}", margin, y + 14f, textPaint)
                cv.drawText("Dicetak: $nowStr", margin + usableWidth - 160f, y, smallTextPaint)
                cv.drawText("Periode: ${summary.monthName} ${summary.year}", margin + usableWidth - 160f, y + 14f, smallTextPaint)

                y += 26f
                cv.drawLine(margin, y, margin + usableWidth, y, linePaint)
                y += 12f

                // 3 Summary Boxes (Pemasukan, Pengeluaran, Arus Kas Bersih)
                val cardWidth = (usableWidth - 16f) / 3f
                val cardHeight = 44f

                // 1. Pemasukan
                bgCardPaint.color = Color.parseColor("#ECFDF5")
                cv.drawRoundRect(RectF(margin, y, margin + cardWidth, y + cardHeight), 5f, 5f, bgCardPaint)
                val pGreen = Paint().apply { color = Color.parseColor("#047857"); textSize = 8f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText("TOTAL PEMASUKAN", margin + 8f, y + 14f, pGreen)
                val valGreen = Paint().apply { color = Color.parseColor("#065F46"); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText(Formatters.formatRupiah(summary.totalIncome), margin + 8f, y + 33f, valGreen)

                // 2. Pengeluaran
                val x2 = margin + cardWidth + 8f
                bgCardPaint.color = Color.parseColor("#FEF2F2")
                cv.drawRoundRect(RectF(x2, y, x2 + cardWidth, y + cardHeight), 5f, 5f, bgCardPaint)
                val pRed = Paint().apply { color = Color.parseColor("#B91C1C"); textSize = 8f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText("TOTAL PENGELUARAN", x2 + 8f, y + 14f, pRed)
                val valRed = Paint().apply { color = Color.parseColor("#991B1B"); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText(Formatters.formatRupiah(summary.totalExpense), x2 + 8f, y + 33f, valRed)

                // 3. Arus Kas Bersih
                val x3 = x2 + cardWidth + 8f
                bgCardPaint.color = if (summary.netSavings >= 0) Color.parseColor("#F0FDF4") else Color.parseColor("#FFF1F2")
                cv.drawRoundRect(RectF(x3, y, x3 + cardWidth, y + cardHeight), 5f, 5f, bgCardPaint)
                val pTeal = Paint().apply { color = if (summary.netSavings >= 0) Color.parseColor("#0F766E") else Color.parseColor("#BE123C"); textSize = 8f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                cv.drawText("ARUS KAS BERSIH", x3 + 8f, y + 14f, pTeal)
                val valTeal = Paint().apply { color = if (summary.netSavings >= 0) Color.parseColor("#115E59") else Color.parseColor("#9F1239"); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                val prefix = if (summary.netSavings >= 0) "+" else ""
                cv.drawText(prefix + Formatters.formatRupiah(summary.netSavings), x3 + 8f, y + 33f, valTeal)

                y += cardHeight + 14f
            }

            return y
        }

        var currentY = drawHeader(canvas, isFirstPage = true)

        // Draw Table Header
        fun drawTableHeader(cv: Canvas, y: Float): Float {
            bgCardPaint.color = Color.parseColor("#F3F4F6")
            cv.drawRoundRect(RectF(margin, y, margin + usableWidth, y + 18f), 3f, 3f, bgCardPaint)

            val thPaint = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            cv.drawText("No", margin + 6f, y + 12f, thPaint)
            cv.drawText("Tgl / Jam", margin + 26f, y + 12f, thPaint)
            cv.drawText("Keterangan", margin + 90f, y + 12f, thPaint)
            cv.drawText("Produk/Kategori", margin + 245f, y + 12f, thPaint)
            cv.drawText("Metode", margin + 355f, y + 12f, thPaint)
            cv.drawText("Tipe", margin + 415f, y + 12f, thPaint)

            val thAlignRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }
            cv.drawText("Nominal (Rp)", margin + usableWidth - 6f, y + 12f, thAlignRight)

            return y + 22f
        }

        currentY = drawTableHeader(canvas, currentY)

        val rowHeight = 18f
        val sortedTransactions = transactions.sortedBy { it.timestamp }

        sortedTransactions.forEachIndexed { index, tx ->
            // Check if we need to start a new page
            if (currentY + rowHeight > pageHeight - 50f) {
                // Page footer
                cvDrawFooter(canvas, currentPageNumber, margin, usableWidth, pageHeight, smallTextPaint)
                document.finishPage(page)

                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                currentY = drawHeader(canvas, isFirstPage = false)
                currentY = drawTableHeader(canvas, currentY)
            }

            // Alternating row background
            if (index % 2 == 1) {
                bgCardPaint.color = Color.parseColor("#F9FAFB")
                canvas.drawRect(RectF(margin, currentY - 3f, margin + usableWidth, currentY + rowHeight - 3f), bgCardPaint)
            }

            val prod = ProductRegistry.getProduct(tx.categoryId, customProducts)
            val wallet = WalletType.fromId(tx.walletId)
            val isIncome = tx.type == "INCOME"

            canvas.drawText("${index + 1}", margin + 6f, currentY + 9f, textPaint)
            val dateStr = "${dfDate.format(Date(tx.timestamp))} ${dfTime.format(Date(tx.timestamp))}"
            canvas.drawText(dateStr, margin + 26f, currentY + 9f, textPaint)

            val shortTitle = if (tx.title.length > 28) tx.title.take(26) + "..." else tx.title
            canvas.drawText(shortTitle, margin + 90f, currentY + 9f, boldPaint)

            val shortCat = if (prod.title.length > 20) prod.title.take(18) + "..." else prod.title
            canvas.drawText(shortCat, margin + 245f, currentY + 9f, textPaint)

            canvas.drawText(wallet.title, margin + 355f, currentY + 9f, textPaint)

            val typePaint = Paint().apply {
                color = if (isIncome) Color.parseColor("#059669") else Color.parseColor("#DC2626")
                textSize = 7.5f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.drawText(if (isIncome) "MASUK" else "KELUAR", margin + 415f, currentY + 9f, typePaint)

            val amountPaint = Paint().apply {
                color = if (isIncome) Color.parseColor("#047857") else Color.parseColor("#B91C1C")
                textSize = 8.5f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            val sign = if (isIncome) "+" else "-"
            canvas.drawText(sign + Formatters.formatRupiah(tx.amount), margin + usableWidth - 6f, currentY + 9f, amountPaint)

            currentY += rowHeight
        }

        // Summary Total Row
        if (currentY + 28f > pageHeight - 50f) {
            cvDrawFooter(canvas, currentPageNumber, margin, usableWidth, pageHeight, smallTextPaint)
            document.finishPage(page)

            currentPageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = drawHeader(canvas, isFirstPage = false)
        }

        canvas.drawLine(margin, currentY, margin + usableWidth, currentY, linePaint)
        currentY += 8f

        bgCardPaint.color = Color.parseColor("#F3F4F6")
        canvas.drawRoundRect(RectF(margin, currentY, margin + usableWidth, currentY + 22f), 3f, 3f, bgCardPaint)

        canvas.drawText("TOTAL (${transactions.size} Transaksi)", margin + 8f, currentY + 14f, boldPaint)

        val totalAlignRight = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val netText = "Masuk: ${Formatters.formatCompactRupiah(summary.totalIncome)}  |  Keluar: ${Formatters.formatCompactRupiah(summary.totalExpense)}  |  Bersih: ${Formatters.formatCompactRupiah(summary.netSavings)}"
        canvas.drawText(netText, margin + usableWidth - 8f, currentY + 14f, totalAlignRight)

        // Draw last page footer
        cvDrawFooter(canvas, currentPageNumber, margin, usableWidth, pageHeight, smallTextPaint)
        document.finishPage(page)

        // Write to file
        val fileName = "Laporan_Keuangan_${summary.monthName}_${summary.year}.pdf"
            .replace(" ", "_")
        val outFile = File(getReportsDir(context), fileName)
        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return outFile
    }

    private fun cvDrawFooter(
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
        cv.drawText("Dicetak oleh Saletreng Store • Laporan Resmi Internal", margin, y + 2f, paint)

        val pagePaint = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
        cv.drawText("Hal $pageNum", margin + usableWidth, y + 2f, pagePaint)
    }

    /**
     * Generates a CSV file formatted for Microsoft Excel & Google Sheets with UTF-8 BOM.
     */
    fun exportToExcel(
        context: Context,
        summary: MonthlySummary,
        transactions: List<TransactionEntity>,
        customProducts: List<ProductEntity>,
        userProfile: UserProfile
    ): File {
        val fileName = "Laporan_Pembukuan_${summary.monthName}_${summary.year}.csv"
            .replace(" ", "_")
        val outFile = File(getReportsDir(context), fileName)

        val dfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dfTime = SimpleDateFormat("HH:mm:ss", Locale.US)
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val sb = StringBuilder()
        // UTF-8 BOM for Microsoft Excel compatibility
        sb.append('\uFEFF')

        fun esc(value: String): String {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }

        // Header Section
        sb.append(esc("SALETRENG STORE - LAPORAN PEMBUKUAN KEUANGAN")).append("\n")
        sb.append(esc("Nama Usaha")).append(",").append(esc(userProfile.businessName)).append("\n")
        sb.append(esc("Pemilik")).append(",").append(esc(userProfile.name)).append("\n")
        sb.append(esc("Email")).append(",").append(esc(userProfile.email)).append("\n")
        sb.append(esc("Periode")).append(",").append(esc("${summary.monthName} ${summary.year}")).append("\n")
        sb.append(esc("Tanggal Unduh")).append(",").append(esc(nowStr)).append("\n")
        sb.append("\n")

        // Summary Section
        sb.append(esc("RINGKASAN ARUS KAS")).append("\n")
        sb.append(esc("Total Pemasukan")).append(",").append(String.format(Locale.US, "%.0f", summary.totalIncome)).append("\n")
        sb.append(esc("Total Pengeluaran")).append(",").append(String.format(Locale.US, "%.0f", summary.totalExpense)).append("\n")
        sb.append(esc("Arus Kas Bersih")).append(",").append(String.format(Locale.US, "%.0f", summary.netSavings)).append("\n")
        sb.append(esc("Tingkat Tabungan (%)")).append(",").append(String.format(Locale.US, "%.2f", summary.savingsRate)).append("\n")
        sb.append(esc("Rata-rata Pengeluaran Harian")).append(",").append(String.format(Locale.US, "%.0f", summary.dailyAverageExpense)).append("\n")
        sb.append("\n")

        // Products Breakdown
        if (summary.productBreakdown.isNotEmpty()) {
            sb.append(esc("RINCIAN PENGELUARAN BERDASARKAN PRODUK")).append("\n")
            sb.append(esc("No")).append(",")
                .append(esc("Nama Produk")).append(",")
                .append(esc("Tipe")).append(",")
                .append(esc("Total Pengeluaran (Rp)")).append(",")
                .append(esc("Kontribusi (%)")).append("\n")

            summary.productBreakdown.forEachIndexed { i, p ->
                val typeStr = if (p.product.type == com.example.model.TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
                sb.append(i + 1).append(",")
                    .append(esc(p.product.title)).append(",")
                    .append(esc(typeStr)).append(",")
                    .append(String.format(Locale.US, "%.0f", p.totalAmount)).append(",")
                    .append(String.format(Locale.US, "%.2f", p.percentage)).append("\n")
            }
            sb.append("\n")
        }

        // Full Transaction Table
        sb.append(esc("DAFTAR LENGKAP TRANSAKSI")).append("\n")
        sb.append(esc("No")).append(",")
            .append(esc("Tanggal")).append(",")
            .append(esc("Waktu")).append(",")
            .append(esc("Judul Transaksi")).append(",")
            .append(esc("Produk / Kategori")).append(",")
            .append(esc("Metode / Dompet")).append(",")
            .append(esc("Tipe Transaksi")).append(",")
            .append(esc("Pemasukan (Rp)")).append(",")
            .append(esc("Pengeluaran (Rp)")).append(",")
            .append(esc("Catatan")).append("\n")

        val sorted = transactions.sortedBy { it.timestamp }
        var sumIn = 0.0
        var sumOut = 0.0

        sorted.forEachIndexed { i, tx ->
            val isIncome = tx.type == "INCOME"
            val prod = ProductRegistry.getProduct(tx.categoryId, customProducts)
            val wallet = WalletType.fromId(tx.walletId)

            val inVal = if (isIncome) { sumIn += tx.amount; tx.amount } else 0.0
            val outVal = if (!isIncome) { sumOut += tx.amount; tx.amount } else 0.0

            sb.append(i + 1).append(",")
                .append(esc(dfDate.format(Date(tx.timestamp)))).append(",")
                .append(esc(dfTime.format(Date(tx.timestamp)))).append(",")
                .append(esc(tx.title)).append(",")
                .append(esc(prod.title)).append(",")
                .append(esc(wallet.title)).append(",")
                .append(esc(if (isIncome) "PEMASUKAN" else "PENGELUARAN")).append(",")
                .append(String.format(Locale.US, "%.0f", inVal)).append(",")
                .append(String.format(Locale.US, "%.0f", outVal)).append(",")
                .append(esc(tx.note)).append("\n")
        }

        // Total Row
        sb.append(esc("TOTAL")).append(",")
            .append(esc("")).append(",")
            .append(esc("")).append(",")
            .append(esc("${transactions.size} Transaksi")).append(",")
            .append(esc("")).append(",")
            .append(esc("")).append(",")
            .append(esc("")).append(",")
            .append(String.format(Locale.US, "%.0f", sumIn)).append(",")
            .append(String.format(Locale.US, "%.0f", sumOut)).append(",")
            .append(esc("Arus Kas Bersih: " + String.format(Locale.US, "%.0f", sumIn - sumOut))).append("\n")

        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }

    /**
     * Shares the generated file via Android system share sheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension.replace("_", " "))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title)
        context.startActivity(chooser)
    }

    /**
     * Opens or previews the file with an external application (PDF viewer / Excel).
     */
    fun openFile(context: Context, file: File, mimeType: String, fallbackTitle: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareFile(context, file, mimeType, fallbackTitle)
        }
    }
}
