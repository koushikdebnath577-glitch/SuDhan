package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.Customer
import com.example.data.model.CustomerType
import com.example.data.model.InterestPeriod
import com.example.data.model.InterestType
import com.example.data.model.Loan
import com.example.data.model.LoanCalculatedSummary
import com.example.data.model.LoanType
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentType
import com.example.data.model.RepaymentFrequency
import com.example.ui.components.AddCustomerDialog
import com.example.ui.theme.FinancialAmber
import com.example.ui.theme.FinancialBlue
import com.example.ui.theme.FinancialGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppStrings
import com.example.util.Formatters
import com.example.util.InterestCalculator
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    initialTab: Int = 0,
    preselectedLoanId: Long? = null,
    onTransactionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val language by viewModel.appLanguage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top 3 Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Money Lent", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Money Borrowed", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Record Payment", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        when (selectedTab) {
            0 -> NewLoanForm(
                loanType = LoanType.LENT,
                viewModel = viewModel,
                language = language,
                onSuccess = onTransactionComplete
            )
            1 -> NewLoanForm(
                loanType = LoanType.BORROWED,
                viewModel = viewModel,
                language = language,
                onSuccess = onTransactionComplete
            )
            2 -> RecordPaymentForm(
                viewModel = viewModel,
                preselectedLoanId = preselectedLoanId,
                language = language,
                onSuccess = onTransactionComplete
            )
        }
    }
}

@Composable
fun NewLoanForm(
    loanType: LoanType,
    viewModel: MainViewModel,
    language: AppLanguage,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customerSummaries.collectAsState()
    val defaultInterestType by viewModel.defaultInterestType.collectAsState()
    val defaultInterestPeriod by viewModel.defaultInterestPeriod.collectAsState()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerNameInput by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") }
    var customerAddressInput by remember { mutableStateOf("") }

    var principalInput by remember { mutableStateOf("") }
    var interestRateInput by remember { mutableStateOf("2.0") }
    var interestPeriod by remember { mutableStateOf(defaultInterestPeriod) }
    var interestType by remember { mutableStateOf(defaultInterestType) }
    var repaymentFrequency by remember { mutableStateOf(RepaymentFrequency.MONTHLY) }
    var notesInput by remember { mutableStateOf("") }

    val cal = Calendar.getInstance()
    var startDateMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var dueDateMs by remember { mutableStateOf<Long?>(System.currentTimeMillis() + 90L * 86_400_000L) }

    var showCustomerPickerDropdown by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    var showInterestPeriodDropdown by remember { mutableStateOf(false) }
    var showInterestTypeDropdown by remember { mutableStateOf(false) }
    var showRepaymentFrequencyDropdown by remember { mutableStateOf(false) }

    // Live Calculation Preview
    val principal = principalInput.toDoubleOrNull() ?: 0.0
    val rate = interestRateInput.toDoubleOrNull() ?: 0.0
    val durationDays = if (dueDateMs != null && dueDateMs!! > startDateMs) {
        ((dueDateMs!! - startDateMs) / 86_400_000L).toInt()
    } else 30

    val previewInterest = InterestCalculator.calculateAccruedInterest(
        principal = principal,
        ratePercent = rate,
        period = interestPeriod,
        type = interestType,
        startDateMs = startDateMs,
        targetDateMs = startDateMs + (durationDays * 86_400_000L)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        item {
            Text(
                text = if (loanType == LoanType.LENT) "New Lending Loan Details" else "New Borrowing Record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (loanType == LoanType.LENT) FinancialGreen else FinancialAmber
            )
        }

        // Customer Selection or Quick Fill
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (loanType == LoanType.LENT) "Customer / Borrower" else "Financier / Lender",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        TextButton(onClick = { showCustomerPickerDropdown = true }) {
                            Text("Select Existing")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showCustomerPickerDropdown,
                            onDismissRequest = { showCustomerPickerDropdown = false }
                        ) {
                            customers.forEach { summary ->
                                DropdownMenuItem(
                                    text = { Text("${summary.customer.name} (${summary.customer.phone})") },
                                    onClick = {
                                        selectedCustomer = summary.customer
                                        customerNameInput = summary.customer.name
                                        customerPhoneInput = summary.customer.phone
                                        customerAddressInput = summary.customer.address
                                        showCustomerPickerDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customerNameInput,
                        onValueChange = {
                            customerNameInput = it
                            selectedCustomer = null
                        },
                        label = { Text("Person / Business Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerPhoneInput,
                            onValueChange = { customerPhoneInput = it },
                            label = { Text("Mobile Number *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customerAddressInput,
                            onValueChange = { customerAddressInput = it },
                            label = { Text("Address / City") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Loan Financial Parameters
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Loan & Interest Terms", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = principalInput,
                        onValueChange = { principalInput = it },
                        label = { Text("Principal Amount (₹) *") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = interestRateInput,
                            onValueChange = { interestRateInput = it },
                            label = { Text("Interest Rate (%)") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Interest Period dropdown (Daily, Monthly, Yearly)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = interestPeriod.name.lowercase().replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Period") },
                                trailingIcon = {
                                    IconButton(onClick = { showInterestPeriodDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showInterestPeriodDropdown = true }
                            )

                            DropdownMenu(
                                expanded = showInterestPeriodDropdown,
                                onDismissRequest = { showInterestPeriodDropdown = false }
                            ) {
                                InterestPeriod.values().forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            interestPeriod = p
                                            showInterestPeriodDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Interest Calculation Type dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = interestType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Interest Calculation Method") },
                            trailingIcon = {
                                IconButton(onClick = { showInterestTypeDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showInterestTypeDropdown = true }
                        )

                        DropdownMenu(
                            expanded = showInterestTypeDropdown,
                            onDismissRequest = { showInterestTypeDropdown = false }
                        ) {
                            InterestType.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        interestType = t
                                        showInterestTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Dates (Start Date & Due Date)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = startDateMs }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        c.set(y, m, d)
                                        startDateMs = c.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Start: ${Formatters.formatShortDate(startDateMs)}", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = dueDateMs ?: System.currentTimeMillis() }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        c.set(y, m, d)
                                        dueDateMs = c.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Due: ${if (dueDateMs != null) Formatters.formatShortDate(dueDateMs!!) else "None"}", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes / Purpose") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Live Calculation Summary Preview
        if (principal > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Estimated Interest Calculation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Period ($durationDays days):", fontSize = 12.sp)
                            Text(Formatters.formatCurrency(previewInterest), fontWeight = FontWeight.Bold, color = FinancialAmber)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Estimated Payable:", fontSize = 12.sp)
                            Text(Formatters.formatCurrency(principal + previewInterest), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    if (customerNameInput.isBlank()) return@Button
                    if (principal <= 0) return@Button

                    viewModel.createLoan(
                        customerName = customerNameInput.trim(),
                        customerPhone = customerPhoneInput.trim(),
                        customerAddress = customerAddressInput.trim(),
                        loanType = loanType,
                        principalAmount = principal,
                        startDate = startDateMs,
                        dueDate = dueDateMs,
                        interestRate = rate,
                        interestPeriod = interestPeriod,
                        interestType = interestType,
                        repaymentFrequency = repaymentFrequency,
                        notes = notesInput.trim(),
                        existingCustomerId = selectedCustomer?.id,
                        onDone = { onSuccess() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (loanType == LoanType.LENT) FinancialGreen else FinancialAmber
                )
            ) {
                Text(
                    text = if (loanType == LoanType.LENT) "Record Lent Loan (₹${Formatters.formatCurrency(principal)})" else "Save Borrowed Record",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun RecordPaymentForm(
    viewModel: MainViewModel,
    preselectedLoanId: Long? = null,
    language: AppLanguage,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val calculatedLoans by viewModel.calculatedLoans.collectAsState()

    val activeLoans = calculatedLoans.filter { it.totalBalance > 0.5 }
    var selectedLoanSummary by remember {
        mutableStateOf(activeLoans.find { it.loan.id == preselectedLoanId } ?: activeLoans.firstOrNull())
    }

    var amountInput by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf(PaymentType.PRINCIPAL_AND_INTEREST) }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var paymentDateMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var notesInput by remember { mutableStateOf("") }

    var customPrincipalComponent by remember { mutableStateOf("") }
    var customInterestComponent by remember { mutableStateOf("") }
    var isManualSplit by remember { mutableStateOf(false) }

    var showLoanPickerDropdown by remember { mutableStateOf(false) }
    var showPaymentTypeDropdown by remember { mutableStateOf(false) }
    var showPaymentModeDropdown by remember { mutableStateOf(false) }

    val amount = amountInput.toDoubleOrNull() ?: 0.0

    // Auto Split Logic
    val autoSplit = remember(amount, selectedLoanSummary) {
        val summary = selectedLoanSummary
        if (summary != null && amount > 0) {
            val outstandingInt = summary.outstandingInterest
            if (amount <= outstandingInt) {
                Pair(0.0, amount)
            } else {
                val intPart = outstandingInt
                val princPart = amount - outstandingInt
                Pair(princPart, intPart)
            }
        } else {
            Pair(0.0, 0.0)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Record Payment / Collection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinancialBlue
            )
        }

        // Loan Account Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Select Loan Account *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedLoanSummary?.let { "${it.loan.personName} (Bal: ${Formatters.formatCurrency(it.totalBalance)})" } ?: "Select an active loan",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showLoanPickerDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLoanPickerDropdown = true }
                        )

                        DropdownMenu(
                            expanded = showLoanPickerDropdown,
                            onDismissRequest = { showLoanPickerDropdown = false }
                        ) {
                            activeLoans.forEach { s ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${s.loan.personName} - ${if (s.loan.loanType == LoanType.LENT) "Lent" else "Borrowed"}", fontWeight = FontWeight.Bold)
                                            Text("Balance: ${Formatters.formatCurrency(s.totalBalance)} (Int: ${Formatters.formatCurrency(s.outstandingInterest)})", fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedLoanSummary = s
                                        showLoanPickerDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Amount & Date
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Payment Amount (₹) *") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Payment Mode Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = paymentMode.name.replace("_", " "),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mode") },
                                trailingIcon = {
                                    IconButton(onClick = { showPaymentModeDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPaymentModeDropdown = true }
                            )

                            DropdownMenu(
                                expanded = showPaymentModeDropdown,
                                onDismissRequest = { showPaymentModeDropdown = false }
                            ) {
                                PaymentMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.name.replace("_", " ")) },
                                        onClick = {
                                            paymentMode = mode
                                            showPaymentModeDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Payment Date Button
                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = paymentDateMs }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        c.set(y, m, d)
                                        paymentDateMs = c.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Formatters.formatShortDate(paymentDateMs), fontSize = 12.sp)
                        }
                    }

                    // Auto Split vs Manual Allocation
                    if (amount > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Component Breakdown:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Principal Component: ${Formatters.formatCurrency(if (isManualSplit) (customPrincipalComponent.toDoubleOrNull() ?: 0.0) else autoSplit.first)}", fontSize = 12.sp)
                                    Text("Interest: ${Formatters.formatCurrency(if (isManualSplit) (customInterestComponent.toDoubleOrNull() ?: 0.0) else autoSplit.second)}", fontSize = 12.sp, color = FinancialAmber)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Payment Notes / Reference No.") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    val summary = selectedLoanSummary ?: return@Button
                    if (amount <= 0) return@Button

                    val principalComp = if (isManualSplit) (customPrincipalComponent.toDoubleOrNull() ?: 0.0) else autoSplit.first
                    val interestComp = if (isManualSplit) (customInterestComponent.toDoubleOrNull() ?: 0.0) else autoSplit.second

                    viewModel.recordPayment(
                        loanId = summary.loan.id,
                        amount = amount,
                        paymentDate = paymentDateMs,
                        paymentType = paymentType,
                        principalComponent = principalComp,
                        interestComponent = interestComp,
                        paymentMode = paymentMode,
                        notes = notesInput.trim(),
                        onDone = { onSuccess() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinancialBlue)
            ) {
                Text("Confirm Payment (₹${Formatters.formatCurrency(amount)})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
