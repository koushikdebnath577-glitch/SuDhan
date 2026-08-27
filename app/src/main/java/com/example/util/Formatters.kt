package com.example.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object Formatters {

    private val indianFormat = DecimalFormat("##,##,##0.00").apply {
        decimalFormatSymbols = decimalFormatSymbols.apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
    }

    private val wholeIndianFormat = DecimalFormat("##,##,##0").apply {
        decimalFormatSymbols = decimalFormatSymbols.apply {
            groupingSeparator = ','
        }
    }

    fun formatCurrency(amount: Double, showDecimals: Boolean = false): String {
        val formatted = if (showDecimals || amount % 1 != 0.0) {
            indianFormat.format(amount)
        } else {
            wholeIndianFormat.format(amount)
        }
        return "₹$formatted"
    }

    fun formatDate(timestampMs: Long, pattern: String = "dd MMM yyyy"): String {
        if (timestampMs <= 0) return "-"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatShortDate(timestampMs: Long): String {
        return formatDate(timestampMs, "dd/MM/yy")
    }

    fun formatRelativeDueDate(timestampMs: Long?): String {
        if (timestampMs == null || timestampMs <= 0) return "No Due Date"
        val now = System.currentTimeMillis()
        val diff = timestampMs - now
        val days = (diff / TimeUnit.DAYS.toMillis(1)).toInt()

        return when {
            days == 0 -> "Due Today"
            days == 1 -> "Due Tomorrow"
            days > 1 -> "Due in $days days"
            days == -1 -> "1 day overdue"
            else -> "${abs(days)} days overdue"
        }
    }

    fun formatPercentage(rate: Double): String {
        return if (rate % 1 == 0.0) "${rate.toInt()}%" else "%.2f%%".format(rate)
    }
}
