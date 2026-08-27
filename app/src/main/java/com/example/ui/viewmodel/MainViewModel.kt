package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeSetting
import com.example.data.model.Customer
import com.example.data.model.CustomerFinancialSummary
import com.example.data.model.CustomerType
import com.example.data.model.DashboardSummary
import com.example.data.model.InterestPeriod
import com.example.data.model.InterestType
import com.example.data.model.Loan
import com.example.data.model.LoanCalculatedSummary
import com.example.data.model.LoanStatus
import com.example.data.model.LoanType
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentType
import com.example.data.model.RepaymentFrequency
import com.example.data.model.TransactionItem
import com.example.data.notification.ReminderItem
import com.example.data.notification.ReminderManager
import com.example.data.repository.LendingRepository
import com.example.data.security.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

enum class LoanFilterOption {
    ALL,
    ACTIVE,
    COMPLETED,
    OVERDUE,
    LENT,
    BORROWED
}

enum class ReportTimeRange {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = LendingRepository(
        customerDao = database.customerDao(),
        loanDao = database.loanDao(),
        paymentDao = database.paymentDao()
    )
    val securityPreferences = SecurityPreferences(application)

    // Security Unlock State in Session
    private val _isAppUnlocked = MutableStateFlow(!securityPreferences.isPinLockEnabled.value)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Global Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLoanFilter = MutableStateFlow(LoanFilterOption.ALL)
    val selectedLoanFilter: StateFlow<LoanFilterOption> = _selectedLoanFilter.asStateFlow()

    private val _selectedReportRange = MutableStateFlow(ReportTimeRange.THIS_MONTH)
    val selectedReportRange: StateFlow<ReportTimeRange> = _selectedReportRange.asStateFlow()

    // Toast / Message feedback
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // App Preferences State
    val appLanguage: StateFlow<AppLanguage> = securityPreferences.appLanguage
    val appTheme: StateFlow<AppThemeSetting> = securityPreferences.appTheme
    val isPinLockEnabled: StateFlow<Boolean> = securityPreferences.isPinLockEnabled
    val defaultInterestType: StateFlow<InterestType> = securityPreferences.defaultInterestType
    val defaultInterestPeriod: StateFlow<InterestPeriod> = securityPreferences.defaultInterestPeriod

    // Core Data Flows
    val dashboardSummary: StateFlow<DashboardSummary> = repository.dashboardSummaryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    val customerSummaries: StateFlow<List<CustomerFinancialSummary>> = repository.customerSummariesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calculatedLoans: StateFlow<List<LoanCalculatedSummary>> = repository.calculatedLoansFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionItem>> = repository.unifiedTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderItem>> = calculatedLoans.combine(
        MutableStateFlow(Unit)
    ) { loans, _ ->
        ReminderManager.generateReminders(loans)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        ReminderManager.initNotificationChannel(application)
        seedSampleDataIfEmpty()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLoanFilter(filter: LoanFilterOption) {
        _selectedLoanFilter.value = filter
    }

    fun setReportRange(range: ReportTimeRange) {
        _selectedReportRange.value = range
    }

    fun unlockApp(pin: String): Boolean {
        val success = securityPreferences.verifyPin(pin)
        if (success) {
            _isAppUnlocked.value = true
        }
        return success
    }

    fun lockApp() {
        if (securityPreferences.isPinLockEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun setPin(enabled: Boolean, pin: String? = null) {
        securityPreferences.setPinLock(enabled, pin)
        if (!enabled) {
            _isAppUnlocked.value = true
        }
    }

    fun setLanguage(language: AppLanguage) {
        securityPreferences.setLanguage(language)
    }

    fun setTheme(theme: AppThemeSetting) {
        securityPreferences.setTheme(theme)
    }

    fun setDefaultInterestType(type: InterestType) {
        securityPreferences.setDefaultInterestType(type)
    }

    fun setDefaultInterestPeriod(period: InterestPeriod) {
        securityPreferences.setDefaultInterestPeriod(period)
    }

    fun addCustomer(customer: Customer, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertCustomer(customer)
            showToast("Customer added successfully")
            onDone(id)
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            showToast("Customer updated")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            showToast("Customer deleted")
        }
    }

    fun createLoan(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        loanType: LoanType,
        principalAmount: Double,
        startDate: Long,
        dueDate: Long?,
        interestRate: Double,
        interestPeriod: InterestPeriod,
        interestType: InterestType,
        repaymentFrequency: RepaymentFrequency,
        notes: String,
        existingCustomerId: Long? = null,
        onDone: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val loanId = repository.createLoan(
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                loanType = loanType,
                principalAmount = principalAmount,
                startDate = startDate,
                dueDate = dueDate,
                interestRate = interestRate,
                interestPeriod = interestPeriod,
                interestType = interestType,
                repaymentFrequency = repaymentFrequency,
                notes = notes,
                existingCustomerId = existingCustomerId
            )
            showToast(if (loanType == LoanType.LENT) "Lending loan created" else "Borrowing record created")
            onDone(loanId)
        }
    }

    fun recordPayment(
        loanId: Long,
        amount: Double,
        paymentDate: Long,
        paymentType: PaymentType,
        principalComponent: Double,
        interestComponent: Double,
        paymentMode: PaymentMode,
        notes: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordPayment(
                loanId = loanId,
                amount = amount,
                paymentDate = paymentDate,
                paymentType = paymentType,
                principalComponent = principalComponent,
                interestComponent = interestComponent,
                paymentMode = paymentMode,
                notes = notes
            )
            showToast("Payment recorded successfully")
            onDone()
        }
    }

    fun updatePayment(payment: Payment) {
        viewModelScope.launch {
            repository.updatePayment(payment)
            showToast("Payment updated & balances recalculated")
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            showToast("Payment deleted & balances recalculated")
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            showToast("Loan deleted")
        }
    }

    fun createBackup(context: Context) {
        viewModelScope.launch {
            try {
                val file = BackupManager.createJsonBackup(context, repository)
                BackupManager.shareFile(context, file, "application/json")
                showToast("Backup created: ${file.name}")
            } catch (e: Exception) {
                showToast("Backup failed: ${e.message}")
            }
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            val result = BackupManager.restoreFromJson(jsonString, repository)
            if (result.isSuccess) {
                showToast(result.getOrDefault("Database restored!"))
            } else {
                showToast("Restore failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun exportTransactionsCsv(context: Context) {
        viewModelScope.launch {
            try {
                val txs = transactions.value
                val file = BackupManager.exportTransactionsCsv(context, txs)
                BackupManager.shareFile(context, file, "text/csv")
                showToast("Transactions CSV exported")
            } catch (e: Exception) {
                showToast("Export failed: ${e.message}")
            }
        }
    }

    fun exportReportCsv(
        context: Context,
        reportTitle: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        viewModelScope.launch {
            try {
                val file = BackupManager.exportReportCsv(context, reportTitle, headers, rows)
                BackupManager.shareFile(context, file, "text/csv")
                showToast("$reportTitle exported")
            } catch (e: Exception) {
                showToast("Export failed: ${e.message}")
            }
        }
    }

    private fun seedSampleDataIfEmpty() {
        viewModelScope.launch {
            val customers = repository.allCustomers.first()
            if (customers.isEmpty()) {
                val now = System.currentTimeMillis()
                val oneDay = 86_400_000L
                val oneMonth = 30 * oneDay

                // 1. Customer: Rajesh Sharma (Borrower - Lent money to him)
                val c1Id = repository.insertCustomer(
                    Customer(
                        name = "Rajesh Sharma",
                        phone = "9876543210",
                        address = "MG Road, Kolkata",
                        type = CustomerType.CUSTOMER,
                        notes = "Hardware store owner, regular borrower"
                    )
                )

                // 2. Customer: Amit Roy (Borrower)
                val c2Id = repository.insertCustomer(
                    Customer(
                        name = "Amit Roy",
                        phone = "9830123456",
                        address = "Salt Lake Sector 5, Kolkata",
                        type = CustomerType.CUSTOMER,
                        notes = "Tech freelancer"
                    )
                )

                // 3. Customer: Subhash Finance (Lender - We borrowed money from them)
                val c3Id = repository.insertCustomer(
                    Customer(
                        name = "Subhash Agrawal",
                        phone = "9821098765",
                        address = "Burrabazar, Kolkata",
                        type = CustomerType.LENDER,
                        notes = "Wholesale financier"
                    )
                )

                // 4. Customer: Priya Banerjee
                val c4Id = repository.insertCustomer(
                    Customer(
                        name = "Priya Banerjee",
                        phone = "9804567890",
                        address = "Gariahat, Kolkata",
                        type = CustomerType.CUSTOMER,
                        notes = "Boutique owner"
                    )
                )

                // Loan 1: Rajesh Sharma (Lent ₹50,000, 2% monthly simple, 3 months ago)
                val l1Id = repository.createLoan(
                    customerName = "Rajesh Sharma",
                    customerPhone = "9876543210",
                    customerAddress = "MG Road, Kolkata",
                    loanType = LoanType.LENT,
                    principalAmount = 50000.0,
                    startDate = now - (75 * oneDay),
                    dueDate = now + (15 * oneDay),
                    interestRate = 2.0,
                    interestPeriod = InterestPeriod.MONTHLY,
                    interestType = InterestType.SIMPLE,
                    repaymentFrequency = RepaymentFrequency.MONTHLY,
                    notes = "Shop inventory expansion loan",
                    existingCustomerId = c1Id
                )
                // Payment on Loan 1 (Interest paid for 2 months = ₹2,000)
                repository.recordPayment(
                    loanId = l1Id,
                    amount = 2000.0,
                    paymentDate = now - (15 * oneDay),
                    paymentType = PaymentType.INTEREST,
                    principalComponent = 0.0,
                    interestComponent = 2000.0,
                    paymentMode = PaymentMode.UPI,
                    notes = "Month 1 & 2 interest via GPay"
                )

                // Loan 2: Amit Roy (Lent ₹1,00,000, 1.5% monthly reducing balance, 4 months ago)
                val l2Id = repository.createLoan(
                    customerName = "Amit Roy",
                    customerPhone = "9830123456",
                    customerAddress = "Salt Lake Sector 5, Kolkata",
                    loanType = LoanType.LENT,
                    principalAmount = 100000.0,
                    startDate = now - (90 * oneDay),
                    dueDate = now + (90 * oneDay),
                    interestRate = 1.5,
                    interestPeriod = InterestPeriod.MONTHLY,
                    interestType = InterestType.REDUCING_BALANCE,
                    repaymentFrequency = RepaymentFrequency.MONTHLY,
                    notes = "Laptop & gear purchase",
                    existingCustomerId = c2Id
                )
                // Payments on Loan 2: Principal ₹20,000 + Interest ₹1,500
                repository.recordPayment(
                    loanId = l2Id,
                    amount = 21500.0,
                    paymentDate = now - (60 * oneDay),
                    paymentType = PaymentType.PRINCIPAL_AND_INTEREST,
                    principalComponent = 20000.0,
                    interestComponent = 1500.0,
                    paymentMode = PaymentMode.BANK_TRANSFER,
                    notes = "Part payment via NEFT"
                )
                repository.recordPayment(
                    loanId = l2Id,
                    amount = 21200.0,
                    paymentDate = now - (30 * oneDay),
                    paymentType = PaymentType.PRINCIPAL_AND_INTEREST,
                    principalComponent = 20000.0,
                    interestComponent = 1200.0,
                    paymentMode = PaymentMode.UPI,
                    notes = "Second installment"
                )

                // Loan 3: Subhash Agrawal (Money Borrowed ₹2,00,000 at 1.0% monthly flat)
                val l3Id = repository.createLoan(
                    customerName = "Subhash Agrawal",
                    customerPhone = "9821098765",
                    customerAddress = "Burrabazar, Kolkata",
                    loanType = LoanType.BORROWED,
                    principalAmount = 200000.0,
                    startDate = now - (60 * oneDay),
                    dueDate = now + (120 * oneDay),
                    interestRate = 1.0,
                    interestPeriod = InterestPeriod.MONTHLY,
                    interestType = InterestType.FLAT,
                    repaymentFrequency = RepaymentFrequency.MONTHLY,
                    notes = "Capital borrowed for lending operations",
                    existingCustomerId = c3Id
                )
                repository.recordPayment(
                    loanId = l3Id,
                    amount = 4000.0,
                    paymentDate = now - (10 * oneDay),
                    paymentType = PaymentType.INTEREST,
                    principalComponent = 0.0,
                    interestComponent = 4000.0,
                    paymentMode = PaymentMode.BANK_TRANSFER,
                    notes = "Interest payment for 2 months"
                )

                // Loan 4: Priya Banerjee (Overdue loan of ₹30,000)
                val l4Id = repository.createLoan(
                    customerName = "Priya Banerjee",
                    customerPhone = "9804567890",
                    customerAddress = "Gariahat, Kolkata",
                    loanType = LoanType.LENT,
                    principalAmount = 30000.0,
                    startDate = now - (60 * oneDay),
                    dueDate = now - (5 * oneDay), // Overdue!
                    interestRate = 2.5,
                    interestPeriod = InterestPeriod.MONTHLY,
                    interestType = InterestType.SIMPLE,
                    repaymentFrequency = RepaymentFrequency.MONTHLY,
                    notes = "Exhibition stall advance",
                    existingCustomerId = c4Id
                )
            }
        }
    }
}
