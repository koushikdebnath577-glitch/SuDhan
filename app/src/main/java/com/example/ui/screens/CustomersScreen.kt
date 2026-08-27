package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.model.Customer
import com.example.data.model.CustomerFinancialSummary
import com.example.data.model.CustomerType
import com.example.data.model.LoanCalculatedSummary
import com.example.data.model.Payment
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.CustomerListItemCard
import com.example.ui.theme.FinancialAmber
import com.example.ui.theme.FinancialBlue
import com.example.ui.theme.FinancialGreen
import com.example.ui.theme.FinancialRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppStrings
import com.example.util.Formatters

enum class CustomerFilterType {
    ALL,
    BORROWERS,
    LENDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: MainViewModel,
    onNavigateToRecordPayment: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val customerSummaries by viewModel.customerSummaries.collectAsState()
    val allLoans by viewModel.calculatedLoans.collectAsState()
    val language by viewModel.appLanguage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CustomerFilterType.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForDetails by remember { mutableStateOf<CustomerFinancialSummary?>(null) }

    val filteredCustomers = customerSummaries.filter { summary ->
        val matchesQuery = summary.customer.name.contains(searchQuery, ignoreCase = true) ||
                summary.customer.phone.contains(searchQuery, ignoreCase = true) ||
                summary.customer.address.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            CustomerFilterType.ALL -> true
            CustomerFilterType.BORROWERS -> summary.customer.type == CustomerType.CUSTOMER || summary.customer.type == CustomerType.BOTH
            CustomerFilterType.LENDERS -> summary.customer.type == CustomerType.LENDER || summary.customer.type == CustomerType.BOTH
        }

        matchesQuery && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customer by name or phone...") },
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

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == CustomerFilterType.ALL,
                    onClick = { selectedFilter = CustomerFilterType.ALL },
                    label = { Text("All (${customerSummaries.size})") }
                )
                FilterChip(
                    selected = selectedFilter == CustomerFilterType.BORROWERS,
                    onClick = { selectedFilter = CustomerFilterType.BORROWERS },
                    label = { Text("Borrowers") }
                )
                FilterChip(
                    selected = selectedFilter == CustomerFilterType.LENDERS,
                    onClick = { selectedFilter = CustomerFilterType.LENDERS },
                    label = { Text("Lenders") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No customers found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers) { summary ->
                        CustomerListItemCard(
                            summary = summary,
                            onClick = { selectedCustomerForDetails = summary }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddDialog) {
        AddCustomerDialog(
            language = language,
            onDismiss = { showAddDialog = false },
            onSave = { customer ->
                viewModel.addCustomer(customer)
                showAddDialog = false
            }
        )
    }

    // Customer Statement & Details Bottom Sheet
    selectedCustomerForDetails?.let { summary ->
        val customerLoans = allLoans.filter { it.loan.personId == summary.customer.id }
        CustomerLedgerSheet(
            summary = summary,
            loans = customerLoans,
            language = language,
            onDismiss = { selectedCustomerForDetails = null },
            onRecordPayment = { loanId ->
                selectedCustomerForDetails = null
                onNavigateToRecordPayment(loanId)
            },
            onDeleteCustomer = {
                viewModel.deleteCustomer(summary.customer)
                selectedCustomerForDetails = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerSheet(
    summary: CustomerFinancialSummary,
    loans: List<LoanCalculatedSummary>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onRecordPayment: (Long) -> Unit,
    onDeleteCustomer: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val customer = summary.customer

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${customer.phone} • ${customer.address.ifBlank { "No address" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareText = "SuDhan Ledger for ${customer.name}\n" +
                                    "Total Outstanding: ${Formatters.formatCurrency(summary.outstandingBalance)}\n" +
                                    "Total Lent: ${Formatters.formatCurrency(summary.totalLent)}\n" +
                                    "Total Repaid: ${Formatters.formatCurrency(summary.totalPrincipalPaid)}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Customer Statement"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Statement")
                    }
                }
            }

            // Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Lent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(Formatters.formatCurrency(summary.totalLent), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Repaid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(Formatters.formatCurrency(summary.totalPrincipalPaid), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FinancialGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Outstanding", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(Formatters.formatCurrency(summary.outstandingBalance), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Loans Ledger History Header
            item {
                Text(
                    text = "Loan Accounts & Ledgers (${loans.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(loans) { loanSummary ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Principal: ${Formatters.formatCurrency(loanSummary.loan.principalAmount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${loanSummary.loan.interestRate}% ${loanSummary.loan.interestType.name.replace("_", " ")}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Accrued Interest: ${Formatters.formatCurrency(loanSummary.accruedInterest)}",
                                fontSize = 12.sp,
                                color = FinancialAmber
                            )
                            Text(
                                text = "Balance: ${Formatters.formatCurrency(loanSummary.totalBalance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (loanSummary.totalBalance > 1.0) FinancialGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (loanSummary.totalBalance > 1.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onRecordPayment(loanSummary.loan.id) },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Record Payment", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDeleteCustomer,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = FinancialRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Customer")
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
