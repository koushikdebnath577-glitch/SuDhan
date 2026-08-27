package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InterestPeriod
import com.example.data.model.InterestType
import com.example.ui.theme.FinancialAmber
import com.example.ui.theme.FinancialBlue
import com.example.ui.theme.FinancialGreen
import com.example.util.Formatters
import com.example.util.InterestCalculator
import kotlin.math.max
import kotlin.math.pow

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier
) {
    var principalInput by remember { mutableStateOf("100000") }
    var interestRateInput by remember { mutableStateOf("2.0") }
    var durationMonthsInput by remember { mutableStateOf("12") }

    var selectedInterestType by remember { mutableStateOf(InterestType.SIMPLE) }
    var selectedPeriod by remember { mutableStateOf(InterestPeriod.MONTHLY) }

    var showTypeDropdown by remember { mutableStateOf(false) }
    var showPeriodDropdown by remember { mutableStateOf(false) }

    val principal = principalInput.toDoubleOrNull() ?: 0.0
    val rate = interestRateInput.toDoubleOrNull() ?: 0.0
    val months = durationMonthsInput.toIntOrNull() ?: 1

    // Calculation result
    val durationDays = (months * 30.4375).toInt()
    val totalInterest = when (selectedInterestType) {
        InterestType.SIMPLE -> {
            val periods = InterestCalculator.daysToPeriods(durationDays.toDouble(), selectedPeriod)
            principal * (rate / 100.0) * periods
        }
        InterestType.COMPOUND -> {
            val periods = InterestCalculator.daysToPeriods(durationDays.toDouble(), selectedPeriod)
            val r = rate / 100.0
            max(0.0, principal * (1.0 + r).pow(periods) - principal)
        }
        InterestType.FLAT -> {
            val periods = InterestCalculator.daysToPeriods(durationDays.toDouble(), selectedPeriod)
            principal * (rate / 100.0) * periods
        }
        InterestType.REDUCING_BALANCE -> {
            val annualRate = when (selectedPeriod) {
                InterestPeriod.MONTHLY -> rate * 12.0
                InterestPeriod.YEARLY -> rate
                InterestPeriod.DAILY -> rate * 365.0
                InterestPeriod.WEEKLY -> rate * 52.0
            }
            val emiRes = InterestCalculator.calculateEmi(principal, annualRate, months)
            emiRes.totalInterest
        }
    }

    val totalPayable = principal + totalInterest
    val monthlyPayment = if (months > 0) totalPayable / months else 0.0

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
                    text = "Smart Interest Calculator",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    principalInput = "100000"
                    interestRateInput = "2.0"
                    durationMonthsInput = "12"
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset")
                }
            }
        }

        // Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = principalInput,
                        onValueChange = { principalInput = it },
                        label = { Text("Principal Amount (₹)") },
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

                        // Period Selector
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedPeriod.name.lowercase().replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Period") },
                                trailingIcon = {
                                    IconButton(onClick = { showPeriodDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPeriodDropdown = true }
                            )

                            DropdownMenu(
                                expanded = showPeriodDropdown,
                                onDismissRequest = { showPeriodDropdown = false }
                            ) {
                                InterestPeriod.values().forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            selectedPeriod = p
                                            showPeriodDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Interest Type
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedInterestType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Calculation Method") },
                            trailingIcon = {
                                IconButton(onClick = { showTypeDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTypeDropdown = true }
                        )

                        DropdownMenu(
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false }
                        ) {
                            InterestType.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        selectedInterestType = t
                                        showTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = durationMonthsInput,
                        onValueChange = { durationMonthsInput = it },
                        label = { Text("Tenure / Duration (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Calculation Result Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calculation Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Interest Payable", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCurrency(totalInterest), fontWeight = FontWeight.Bold, color = FinancialAmber, fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Repayment Amount", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCurrency(totalPayable), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Monthly Installment (Approx.):", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(Formatters.formatCurrency(monthlyPayment) + "/mo", fontWeight = FontWeight.Bold, color = FinancialGreen, fontSize = 15.sp)
                    }
                }
            }
        }

        // Amortization Breakdown Table preview
        item {
            Text(
                text = "Month-by-Month Amortization Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Month", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                        Text("Installment", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                        Text("Interest", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("Balance", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                    }

                    val monthlyInt = totalInterest / months.coerceAtLeast(1)
                    val monthlyPrinc = principal / months.coerceAtLeast(1)
                    var runningBal = totalPayable

                    for (m in 1..months.coerceAtMost(12)) {
                        runningBal -= monthlyPayment
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("M$m", fontSize = 12.sp, modifier = Modifier.weight(0.8f))
                            Text(Formatters.formatCurrency(monthlyPayment), fontSize = 12.sp, modifier = Modifier.weight(1.2f))
                            Text(Formatters.formatCurrency(monthlyInt), fontSize = 12.sp, color = FinancialAmber, modifier = Modifier.weight(1f))
                            Text(Formatters.formatCurrency(runningBal.coerceAtLeast(0.0)), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
                        }
                        if (m < months.coerceAtMost(12)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
