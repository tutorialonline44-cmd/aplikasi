package com.example.util

import com.example.model.MonthlySummary
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    private val indonesianLocale = Locale("id", "ID")

    val MONTH_NAMES = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    fun formatRupiah(amount: Double): String {
        val symbols = DecimalFormatSymbols(indonesianLocale).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedNumber = formatter.format(amount.toLong())
        return "Rp $formattedNumber"
    }

    fun formatRupiahWithSign(amount: Double, isIncome: Boolean): String {
        val prefix = if (isIncome) "+ " else "- "
        return prefix + formatRupiah(amount)
    }

    fun formatCompactRupiah(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> String.format(indonesianLocale, "Rp %.1f M", amount / 1_000_000_000.0)
            amount >= 1_000_000 -> String.format(indonesianLocale, "Rp %.1f jt", amount / 1_000_000.0)
            amount >= 1_000 -> String.format(indonesianLocale, "Rp %.0f rb", amount / 1_000.0)
            else -> formatRupiah(amount)
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatDayDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun getMonthName(monthNumber: Int): String { // 1..12
        val index = (monthNumber - 1).coerceIn(0, 11)
        return MONTH_NAMES[index]
    }

    fun generateShareableReport(summary: MonthlySummary): String {
        val sb = StringBuilder()
        sb.append("📊 LAPORAN KEUANGAN BULANAN - ${summary.monthName.uppercase()} ${summary.year}\n")
        sb.append("Dibuat dengan Aplikasi Saletreng Store\n\n")
        sb.append("💵 Total Pemasukan: ${formatRupiah(summary.totalIncome)}\n")
        sb.append("💸 Total Pengeluaran: ${formatRupiah(summary.totalExpense)}\n")
        val netPrefix = if (summary.netSavings >= 0) "Surplus (+)" else "Defisit (-)"
        sb.append("📈 Selisih Bersih: $netPrefix ${formatRupiah(kotlin.math.abs(summary.netSavings))}\n")
        sb.append("🎯 Tingkat Tabungan: ${String.format(indonesianLocale, "%.1f", summary.savingsRate)}%\n")

        if (summary.productBreakdown.isNotEmpty()) {
            sb.append("\n🏷️ RINCIAN PENGELUARAN PER PRODUK:\n")
            summary.productBreakdown.forEach { item ->
                sb.append("• ${item.product.title}: ${formatRupiah(item.totalAmount)} (${String.format(indonesianLocale, "%.1f", item.percentage)}%)\n")
            }
        }

        sb.append("\n💡 Rata-rata pengeluaran harian: ${formatRupiah(summary.dailyAverageExpense)}")
        return sb.toString()
    }
}
