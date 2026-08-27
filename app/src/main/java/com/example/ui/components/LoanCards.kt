package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerFinancialSummary
import com.example.data.model.CustomerType
import com.example.data.model.LoanCalculatedSummary
import com.example.data.model.LoanStatus
import com.example.data.model.LoanType
import com.example.ui.theme.FinancialAmber
import com.example.ui.theme.FinancialAmberContainer
import com.example.ui.theme.FinancialBlue
import com.example.ui.theme.FinancialBlueContainer
import com.example.ui.theme.FinancialGreen
import com.example.ui.theme.FinancialGreenContainer
import com.example.ui.theme.FinancialRed
import com.example.ui.theme.FinancialRedContainer
import com.example.ui.theme.SleekIndigoPrimary
import com.example.ui.theme.SleekIndigoPrimaryContainer
import com.example.ui.theme.SleekTertiary
import com.example.ui.theme.SleekTertiaryContainer
import com.example.util.Formatters

@Composable
fun LoanItemCard(
    summary: LoanCalculatedSummary,
    onRecordPaymentClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loan = summary.loan
    val isLent = loan.loanType == LoanType.LENT
    val isCompleted = summary.totalBalance <= 1.0 || loan.status == LoanStatus.COMPLETED
    val isOverdue = summary.isOverdue && !isCompleted

    val paidRatio = if (loan.principalAmount > 0) {
        (summary.totalPrincipalPaid / loan.principalAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val initials = loan.personName.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifEmpty { "L" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar Initials, Name, Type Badge, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isLent) SleekIndigoPrimaryContainer else FinancialAmberContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (isLent) SleekIndigoPrimary else FinancialAmber
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = loan.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${loan.interestRate}% ${loan.interestPeriod.name.lowercase().replaceFirstChar { it.uppercase() }} • ${loan.interestType.name.replace("_", " ")}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Status Badge (Sleek Pill)
                StatusBadge(isCompleted = isCompleted, isOverdue = isOverdue, dueDate = loan.dueDate)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Financial Numbers Row (Sleek Sub-surface)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isLent) "Lent Principal" else "Borrowed Amount",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = Formatters.formatCurrency(loan.principalAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Accrued Interest",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = Formatters.formatCurrency(summary.accruedInterest),
                        fontWeight = FontWeight.Bold,
                        color = FinancialAmber,
                        fontSize = 14.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Balance Due",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = Formatters.formatCurrency(summary.totalBalance),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOverdue) FinancialRed else if (isLent) SleekIndigoPrimary else FinancialAmber,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Repayment Progress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Principal Paid: ${Formatters.formatCurrency(summary.totalPrincipalPaid)} (${(paidRatio * 100).toInt()}%)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Start: ${Formatters.formatShortDate(loan.startDate)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { paidRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (isCompleted) FinancialGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (Sleek Pill Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCompleted) {
                    Button(
                        onClick = onRecordPaymentClick,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLent) "Collect Payment" else "Make Payment",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(FinancialGreenContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = FinancialGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Loan Settled",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinancialGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    isCompleted: Boolean,
    isOverdue: Boolean,
    dueDate: Long?
) {
    val (bg, fg, label) = when {
        isCompleted -> Triple(FinancialGreenContainer, FinancialGreen, "Settled")
        isOverdue -> Triple(FinancialRedContainer, FinancialRed, "Overdue")
        dueDate != null -> Triple(FinancialBlueContainer, FinancialBlue, Formatters.formatRelativeDueDate(dueDate))
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Active")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun CustomerListItemCard(
    summary: CustomerFinancialSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customer = summary.customer
    val context = LocalContext.current

    val initials = customer.name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifEmpty { "C" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Initial (Sleek squircle badge)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when (customer.type) {
                            CustomerType.CUSTOMER -> SleekIndigoPrimaryContainer
                            CustomerType.LENDER -> FinancialAmberContainer
                            CustomerType.BOTH -> SleekTertiaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = when (customer.type) {
                        CustomerType.CUSTOMER -> SleekIndigoPrimary
                        CustomerType.LENDER -> FinancialAmber
                        CustomerType.BOTH -> SleekTertiary
                    }
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = customer.phone + if (customer.address.isNotBlank()) " • ${customer.address}" else "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Loans: ${summary.activeLoansCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (summary.completedLoansCount > 0) {
                        Text(
                            text = "Completed: ${summary.completedLoansCount}",
                            fontSize = 11.sp,
                            color = FinancialGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Outstanding Balance & Call Action
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Balance",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = Formatters.formatCurrency(summary.outstandingBalance),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (summary.outstandingBalance > 0) SleekIndigoPrimary else FinancialGreen
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(FinancialGreenContainer)
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                context.startActivity(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = FinancialGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(FinancialBlueContainer)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${customer.phone}"))
                                context.startActivity(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "SMS",
                            tint = FinancialBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
