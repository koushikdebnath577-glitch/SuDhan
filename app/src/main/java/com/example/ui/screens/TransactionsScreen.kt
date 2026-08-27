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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.theme.FinancialAmber
import com.example.ui.theme.FinancialBlue
import com.example.ui.theme.FinancialGreen
import com.example.ui.theme.FinancialPurple
import com.example.ui.theme.FinancialRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppStrings
import com.example.util.Formatters

enum class TxFilterType {
    ALL,
    LOANS_GIVEN,
    LOANS_TAKEN,
    COLLECTIONS,
    PAYMENTS
}

@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val language by viewModel.appLanguage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TxFilterType.ALL) }

    val filtered = transactions.filter { tx ->
        val matchesQuery = tx.personName.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true) ||
                tx.paymentMode.name.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            TxFilterType.ALL -> true
            TxFilterType.LOANS_GIVEN -> tx.transactionType == TransactionType.LOAN_GIVEN
            TxFilterType.LOANS_TAKEN -> tx.transactionType == TransactionType.LOAN_TAKEN
            TxFilterType.COLLECTIONS -> tx.transactionType == TransactionType.PRINCIPAL_COLLECTED || tx.transactionType == TransactionType.INTEREST_COLLECTED
            TxFilterType.PAYMENTS -> tx.transactionType == TransactionType.PRINCIPAL_PAID || tx.transactionType == TransactionType.INTEREST_PAID
        }

        matchesQuery && matchesFilter
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Ledger",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(onClick = { viewModel.exportTransactionsCsv(context) }) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV", fontSize = 12.sp)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TxFilterType.values()) { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = {
                            Text(
                                when (f) {
                                    TxFilterType.ALL -> "All (${transactions.size})"
                                    TxFilterType.LOANS_GIVEN -> "Money Lent"
                                    TxFilterType.LOANS_TAKEN -> "Money Borrowed"
                                    TxFilterType.COLLECTIONS -> "Collections"
                                    TxFilterType.PAYMENTS -> "Repayments"
                                }
                            )
                        }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered) { tx ->
                TransactionItemRow(item = tx)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TransactionItemRow(item: TransactionItem) {
    val (icon, iconColor, typeLabel) = when (item.transactionType) {
        TransactionType.LOAN_GIVEN -> Triple(Icons.Default.ArrowUpward, FinancialGreen, "Money Lent")
        TransactionType.LOAN_TAKEN -> Triple(Icons.Default.ArrowDownward, FinancialAmber, "Money Borrowed")
        TransactionType.PRINCIPAL_COLLECTED -> Triple(Icons.Default.Payments, FinancialBlue, "Principal Collected")
        TransactionType.INTEREST_COLLECTED -> Triple(Icons.Default.Payments, FinancialGreen, "Interest Collected")
        TransactionType.PRINCIPAL_PAID -> Triple(Icons.Default.Payments, FinancialPurple, "Principal Paid")
        TransactionType.INTEREST_PAID -> Triple(Icons.Default.Payments, FinancialRed, "Interest Paid")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.personName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "$typeLabel • ${item.paymentMode.name.replace("_", " ")}",
                    fontSize = 11.sp,
                    color = iconColor,
                    fontWeight = FontWeight.Medium
                )
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCurrency(item.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = when (item.transactionType) {
                        TransactionType.LOAN_GIVEN, TransactionType.INTEREST_PAID, TransactionType.PRINCIPAL_PAID -> FinancialRed
                        else -> FinancialGreen
                    }
                )
                Text(
                    text = Formatters.formatDate(item.date, "dd MMM, hh:mm a"),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
