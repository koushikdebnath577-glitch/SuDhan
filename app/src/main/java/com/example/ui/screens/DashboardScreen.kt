package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.LoanCalculatedSummary
import com.example.data.model.LoanType
import com.example.ui.components.LendingVsBorrowingChart
import com.example.ui.components.LoanItemCard
import com.example.ui.components.MetricCard
import com.example.ui.components.MonthlyProfitBarChart
import com.example.ui.components.PrimaryHeroStatCard
import com.example.ui.theme.FinancialAmber
import com.example.ui.theme.FinancialBlue
import com.example.ui.theme.FinancialGreen
import com.example.ui.theme.FinancialPurple
import com.example.ui.theme.FinancialRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppStrings
import com.example.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToMoneyLent: () -> Unit,
    onNavigateToMoneyBorrowed: () -> Unit,
    onNavigateToRecordPayment: (Long?) -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToCalculator: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onLoanClick: (LoanCalculatedSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val loans by viewModel.calculatedLoans.collectAsState()
    val language by viewModel.appLanguage.collectAsState()

    val overdueLoans = loans.filter { it.isOverdue && it.totalBalance > 1.0 }
    val activeLoans = loans.filter { it.totalBalance > 1.0 }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. Primary Hero Stat Card
        item {
            PrimaryHeroStatCard(
                netProfit = dashboardSummary.netInterestProfit,
                todayCollection = dashboardSummary.todayCollection,
                activeLoansCount = dashboardSummary.activeLoansCount,
                overdueLoansCount = dashboardSummary.overdueLoansCount
            )
        }

        // 2. Quick Action Buttons (Money Lent, Money Borrowed, Record Payment, Calculator)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.ArrowUpward,
                    label = AppStrings.get("money_lent", language),
                    color = FinancialGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMoneyLent
                )
                QuickActionButton(
                    icon = Icons.Default.ArrowDownward,
                    label = AppStrings.get("money_borrowed", language),
                    color = FinancialAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMoneyBorrowed
                )
                QuickActionButton(
                    icon = Icons.Default.Payments,
                    label = AppStrings.get("record_payment", language),
                    color = FinancialBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRecordPayment(null) }
                )
            }
        }

        // 3. Overdue Alert Section (If any exist)
        if (overdueLoans.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FinancialRed.copy(alpha = 0.1f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FinancialRed.copy(alpha = 0.4f)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Overdue",
                                    tint = FinancialRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Overdue Loans Alert (${overdueLoans.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = FinancialRed,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        overdueLoans.take(2).forEach { summary ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = summary.loan.personName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = Formatters.formatRelativeDueDate(summary.loan.dueDate),
                                        fontSize = 11.sp,
                                        color = FinancialRed
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = Formatters.formatCurrency(summary.totalBalance),
                                        fontWeight = FontWeight.Bold,
                                        color = FinancialRed,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { onNavigateToRecordPayment(summary.loan.id) }
                                    ) {
                                        Text("Collect", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FinancialRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Financial Metrics Grid
        item {
            Text(
                text = AppStrings.get("financial_overview", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = AppStrings.get("total_lent", language),
                        amount = dashboardSummary.totalMoneyLent,
                        icon = Icons.Default.CallMade,
                        accentColor = FinancialGreen,
                        subtitle = "Given to Borrowers",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = AppStrings.get("total_borrowed", language),
                        amount = dashboardSummary.totalMoneyBorrowed,
                        icon = Icons.Default.CallReceived,
                        accentColor = FinancialAmber,
                        subtitle = "Taken from Lenders",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = AppStrings.get("principal_outstanding", language),
                        amount = dashboardSummary.totalPrincipalOutstanding,
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = FinancialBlue,
                        subtitle = "Net Capital at Market",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = AppStrings.get("interest_receivable", language),
                        amount = dashboardSummary.totalInterestReceivable,
                        icon = Icons.Default.TrendingUp,
                        accentColor = FinancialGreen,
                        subtitle = "Accrued from Customers",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = AppStrings.get("interest_earned", language),
                        amount = dashboardSummary.totalInterestEarned,
                        icon = Icons.Default.Paid,
                        accentColor = FinancialGreen,
                        subtitle = "Collected Interest",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = AppStrings.get("interest_paid", language),
                        amount = dashboardSummary.totalInterestPaid,
                        icon = Icons.Default.TrendingDown,
                        accentColor = FinancialRed,
                        subtitle = "Paid to Financiers",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Monthly Profit & Loss Visual Bar Chart
        item {
            MonthlyProfitBarChart(points = dashboardSummary.monthlyProfits)
        }

        // 6. Lending vs Borrowing Ratio
        item {
            LendingVsBorrowingChart(
                totalLent = dashboardSummary.totalMoneyLent,
                totalBorrowed = dashboardSummary.totalMoneyBorrowed
            )
        }

        // 7. Active Loans List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Loans & Accounts (${activeLoans.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToTransactions) {
                    Text("View History", fontSize = 13.sp)
                }
            }
        }

        // Active Loans Cards
        items(activeLoans) { summary ->
            LoanItemCard(
                summary = summary,
                onRecordPaymentClick = { onNavigateToRecordPayment(summary.loan.id) },
                onClick = { onLoanClick(summary) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
