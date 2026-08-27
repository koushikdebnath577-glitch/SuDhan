package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.LoanType
import com.example.data.model.TransactionType
import com.example.ui.components.MonthlyProfitBarChart
import com.example.ui.theme.FinancialAmber
import com.example.ui.theme.FinancialBlue
import com.example.ui.theme.FinancialGreen
import com.example.ui.theme.FinancialRed
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ReportTimeRange
import com.example.util.AppStrings
import com.example.util.Formatters
import java.util.Calendar

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val loans by viewModel.calculatedLoans.collectAsState()
    val selectedRange by viewModel.selectedReportRange.collectAsState()
    val language by viewModel.appLanguage.collectAsState()

    // Calculate time bounds
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    val startTime = when (selectedRange) {
        ReportTimeRange.TODAY -> todayStart
        ReportTimeRange.YESTERDAY -> todayStart - 86_400_000L
        ReportTimeRange.THIS_WEEK -> todayStart - (7 * 86_400_000L)
        ReportTimeRange.THIS_MONTH -> todayStart - (30 * 86_400_000L)
        ReportTimeRange.THIS_YEAR -> todayStart - (365 * 86_400_000L)
        ReportTimeRange.ALL -> 0L
    }

    val endTime = when (selectedRange) {
        ReportTimeRange.YESTERDAY -> todayStart
        else -> Long.MAX_VALUE
    }

    val filteredTransactions = transactions.filter { it.date in startTime..endTime }
    val filteredIncome = filteredTransactions
        .filter { it.transactionType == TransactionType.INTEREST_COLLECTED }
        .sumOf { it.amount }

    val filteredExpense = filteredTransactions
        .filter { it.transactionType == TransactionType.INTEREST_PAID }
        .sumOf { it.amount }

    val filteredPrincipalLent = filteredTransactions
        .filter { it.transactionType == TransactionType.LOAN_GIVEN }
        .sumOf { it.amount }

    val filteredPrincipalCollected = filteredTransactions
        .filter { it.transactionType == TransactionType.PRINCIPAL_COLLECTED }
        .sumOf { it.amount }

    val netProfit = filteredIncome - filteredExpense

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("reports_title", language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        val headers = listOf("Date", "Person", "Type", "Amount", "Mode", "Notes")
                        val rows = filteredTransactions.map {
                            listOf(
                                Formatters.formatDate(it.date),
                                it.personName,
                                it.transactionType.name,
                                it.amount.toString(),
                                it.paymentMode.name,
                                it.notes
                            )
                        }
                        viewModel.exportReportCsv(context, "Financial_Report_${selectedRange.name}", headers, rows)
                    }
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("export_csv", language), fontSize = 12.sp)
                }
            }
        }

        // Time Range Filter Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReportTimeRange.values()) { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { viewModel.setReportRange(range) },
                        label = {
                            Text(
                                when (range) {
                                    ReportTimeRange.TODAY -> AppStrings.get("filter_today", language)
                                    ReportTimeRange.YESTERDAY -> AppStrings.get("filter_yesterday", language)
                                    ReportTimeRange.THIS_WEEK -> AppStrings.get("filter_this_week", language)
                                    ReportTimeRange.THIS_MONTH -> AppStrings.get("filter_this_month", language)
                                    ReportTimeRange.THIS_YEAR -> AppStrings.get("filter_this_year", language)
                                    ReportTimeRange.ALL -> AppStrings.get("filter_all", language)
                                }
                            )
                        }
                    )
                }
            }
        }

        // Profit & Loss Highlight Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Profit & Loss Statement (${selectedRange.name.replace("_", " ")})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Interest Income", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCurrency(filteredIncome), fontWeight = FontWeight.Bold, color = FinancialGreen, fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Interest Expense", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCurrency(filteredExpense), fontWeight = FontWeight.Bold, color = FinancialRed, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Interest Profit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = Formatters.formatCurrency(netProfit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (netProfit >= 0) FinancialGreen else FinancialRed
                        )
                    }
                }
            }
        }

        // Cashflow & Capital Movements Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cashflow & Capital Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ReportRowItem(label = "New Principal Lent Out", amount = filteredPrincipalLent, color = FinancialAmber)
                    ReportRowItem(label = "Principal Collected Back", amount = filteredPrincipalCollected, color = FinancialGreen)
                    ReportRowItem(label = "Total Active Loans", amount = loans.count { it.totalBalance > 1.0 }.toDouble(), isCurrency = false, color = MaterialTheme.colorScheme.primary)
                    ReportRowItem(label = "Overdue Loans Count", amount = loans.count { it.isOverdue && it.totalBalance > 1.0 }.toDouble(), isCurrency = false, color = FinancialRed)
                }
            }
        }

        // Monthly Profit Trend
        item {
            MonthlyProfitBarChart(points = dashboardSummary.monthlyProfits)
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ReportRowItem(
    label: String,
    amount: Double,
    isCurrency: Boolean = true,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = if (isCurrency) Formatters.formatCurrency(amount) else "${amount.toInt()}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
        )
    }
}
