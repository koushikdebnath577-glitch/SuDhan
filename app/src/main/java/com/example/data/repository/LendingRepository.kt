package com.example.data.repository

import com.example.data.dao.CustomerDao
import com.example.data.dao.LoanDao
import com.example.data.dao.PaymentDao
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
import com.example.data.model.MonthlyProfitPoint
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentType
import com.example.data.model.RepaymentFrequency
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.util.InterestCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LendingRepository(
    private val customerDao: CustomerDao,
    private val loanDao: LoanDao,
    private val paymentDao: PaymentDao
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allLoans: Flow<List<Loan>> = loanDao.getAllLoans()
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdDirect(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)

    fun getLoanById(id: Long): Flow<Loan?> = loanDao.getLoanById(id)

    fun getPaymentsForLoan(loanId: Long): Flow<List<Payment>> = paymentDao.getPaymentsForLoan(loanId)

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)

    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    suspend fun createLoan(
        customerName: String,
        customerPhone: String,
        customerAddress: String = "",
        loanType: LoanType,
        principalAmount: Double,
        startDate: Long,
        dueDate: Long?,
        interestRate: Double,
        interestPeriod: InterestPeriod,
        interestType: InterestType,
        repaymentFrequency: RepaymentFrequency,
        notes: String,
        existingCustomerId: Long? = null
    ): Long {
        // Find or create customer
        val personId: Long = if (existingCustomerId != null && existingCustomerId > 0) {
            existingCustomerId
        } else {
            val existing = customerDao.searchCustomers(customerPhone)
            // If doesn't exist, insert new
            customerDao.insertCustomer(
                Customer(
                    name = customerName.trim(),
                    phone = customerPhone.trim(),
                    address = customerAddress.trim(),
                    type = if (loanType == LoanType.LENT) CustomerType.CUSTOMER else CustomerType.LENDER
                )
            )
        }

        val loan = Loan(
            personId = personId,
            personName = customerName.trim(),
            personPhone = customerPhone.trim(),
            loanType = loanType,
            principalAmount = principalAmount,
            startDate = startDate,
            dueDate = dueDate,
            interestRate = interestRate,
            interestPeriod = interestPeriod,
            interestType = interestType,
            repaymentFrequency = repaymentFrequency,
            status = LoanStatus.ACTIVE,
            notes = notes.trim()
        )

        val loanId = loanDao.insertLoan(loan)

        // Insert initial disbursement record
        paymentDao.insertPayment(
            Payment(
                loanId = loanId,
                personId = personId,
                personName = customerName.trim(),
                amount = principalAmount,
                paymentDate = startDate,
                paymentType = PaymentType.INITIAL_DISBURSEMENT,
                principalComponent = principalAmount,
                interestComponent = 0.0,
                paymentMode = PaymentMode.CASH,
                notes = if (loanType == LoanType.LENT) "Loan Disbursed to Customer" else "Loan Received from Lender"
            )
        )

        return loanId
    }

    suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(loan)
        recalculateLoanStatus(loan.id)
    }

    suspend fun deleteLoan(loan: Loan) {
        paymentDao.deletePaymentsForLoan(loan.id)
        loanDao.deleteLoan(loan)
    }

    suspend fun recordPayment(
        loanId: Long,
        amount: Double,
        paymentDate: Long,
        paymentType: PaymentType,
        principalComponent: Double,
        interestComponent: Double,
        paymentMode: PaymentMode,
        notes: String
    ): Long {
        val loan = loanDao.getLoanByIdDirect(loanId) ?: return -1

        val paymentId = paymentDao.insertPayment(
            Payment(
                loanId = loanId,
                personId = loan.personId,
                personName = loan.personName,
                amount = amount,
                paymentDate = paymentDate,
                paymentType = paymentType,
                principalComponent = principalComponent,
                interestComponent = interestComponent,
                paymentMode = paymentMode,
                notes = notes
            )
        )

        recalculateLoanStatus(loanId)
        return paymentId
    }

    suspend fun updatePayment(payment: Payment) {
        paymentDao.updatePayment(payment)
        recalculateLoanStatus(payment.loanId)
    }

    suspend fun deletePayment(payment: Payment) {
        paymentDao.deletePayment(payment)
        recalculateLoanStatus(payment.loanId)
    }

    suspend fun recalculateLoanStatus(loanId: Long) {
        val loan = loanDao.getLoanByIdDirect(loanId) ?: return
        val payments = paymentDao.getPaymentsForLoanDirect(loanId)
        val metrics = InterestCalculator.calculateLoanMetrics(loan, payments)

        val newStatus = when {
            metrics.totalBalance <= 1.0 -> LoanStatus.COMPLETED
            metrics.isOverdue -> LoanStatus.OVERDUE
            else -> LoanStatus.ACTIVE
        }

        if (loan.status != newStatus) {
            loanDao.updateLoanStatus(loanId, newStatus)
        }
    }

    /**
     * Combined reactive stream for all loans with calculated metrics
     */
    val calculatedLoansFlow: Flow<List<LoanCalculatedSummary>> = combine(
        allLoans,
        allPayments
    ) { loans, payments ->
        val paymentsByLoan = payments.groupBy { it.loanId }
        val now = System.currentTimeMillis()

        loans.map { loan ->
            val loanPayments = paymentsByLoan[loan.id] ?: emptyList()
            val metrics = InterestCalculator.calculateLoanMetrics(loan, loanPayments, now)

            val status = when {
                metrics.totalBalance <= 1.0 -> LoanStatus.COMPLETED
                metrics.isOverdue -> LoanStatus.OVERDUE
                else -> LoanStatus.ACTIVE
            }

            LoanCalculatedSummary(
                loan = loan.copy(status = status),
                totalPrincipalPaid = metrics.totalPrincipalPaid,
                totalInterestPaid = metrics.totalInterestPaid,
                outstandingPrincipal = metrics.outstandingPrincipal,
                accruedInterest = metrics.accruedInterest,
                outstandingInterest = metrics.outstandingInterest,
                totalPayable = metrics.totalPayable,
                totalBalance = metrics.totalBalance,
                isOverdue = metrics.isOverdue,
                nextPaymentDate = metrics.nextPaymentDate,
                payments = loanPayments
            )
        }
    }

    /**
     * Dashboard Summary Flow
     */
    val dashboardSummaryFlow: Flow<DashboardSummary> = combine(
        calculatedLoansFlow,
        allPayments
    ) { calculatedLoans, payments ->
        val now = Calendar.getInstance()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 86_400_000L

        var totalMoneyLent = 0.0
        var totalMoneyBorrowed = 0.0
        var totalPrincipalOutstanding = 0.0
        var totalInterestReceivable = 0.0
        var totalInterestPayable = 0.0
        var totalInterestEarned = 0.0
        var totalInterestPaid = 0.0

        var activeCount = 0
        var completedCount = 0
        var overdueCount = 0

        calculatedLoans.forEach { summary ->
            val loan = summary.loan
            if (loan.loanType == LoanType.LENT) {
                totalMoneyLent += loan.principalAmount
                totalPrincipalOutstanding += summary.outstandingPrincipal
                totalInterestReceivable += summary.outstandingInterest
                totalInterestEarned += summary.totalInterestPaid
            } else {
                totalMoneyBorrowed += loan.principalAmount
                totalInterestPayable += summary.outstandingInterest
                totalInterestPaid += summary.totalInterestPaid
            }

            when (loan.status) {
                LoanStatus.ACTIVE -> activeCount++
                LoanStatus.COMPLETED -> completedCount++
                LoanStatus.OVERDUE -> {
                    overdueCount++
                    activeCount++
                }
            }
        }

        // Today's collections (payments received on LENT loans today)
        val todayCollection = payments
            .filter { p ->
                p.paymentDate in todayStart..todayEnd &&
                p.paymentType != PaymentType.INITIAL_DISBURSEMENT
            }
            .sumOf { it.amount }

        // Monthly profit points for the last 6 months
        val monthlyPoints = computeMonthlyProfitPoints(payments, calculatedLoans)

        val overdueLoans = calculatedLoans.filter { it.isOverdue && it.loan.status != LoanStatus.COMPLETED }
        val upcomingLoans = calculatedLoans.filter {
            it.loan.status != LoanStatus.COMPLETED &&
            it.nextPaymentDate != null &&
            it.nextPaymentDate > System.currentTimeMillis()
        }.sortedBy { it.nextPaymentDate }

        DashboardSummary(
            totalMoneyLent = totalMoneyLent,
            totalMoneyBorrowed = totalMoneyBorrowed,
            totalPrincipalOutstanding = totalPrincipalOutstanding,
            totalInterestReceivable = totalInterestReceivable,
            totalInterestPayable = totalInterestPayable,
            totalInterestEarned = totalInterestEarned,
            totalInterestPaid = totalInterestPaid,
            netInterestProfit = totalInterestEarned - totalInterestPaid,
            activeLoansCount = activeCount,
            completedLoansCount = completedCount,
            overdueLoansCount = overdueCount,
            todayCollection = todayCollection,
            upcomingPaymentsCount = upcomingLoans.size,
            monthlyProfits = monthlyPoints,
            upcomingLoans = upcomingLoans.take(5),
            overdueLoans = overdueLoans.take(5)
        )
    }

    private fun computeMonthlyProfitPoints(
        payments: List<Payment>,
        loans: List<LoanCalculatedSummary>
    ): List<MonthlyProfitPoint> {
        val sdf = SimpleDateFormat("MMM yy", Locale.getDefault())
        val points = mutableListOf<MonthlyProfitPoint>()
        val cal = Calendar.getInstance()

        // Last 6 months
        for (i in 5 downTo 0) {
            val targetCal = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, -i)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = targetCal.timeInMillis
            val maxDay = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            targetCal.set(Calendar.DAY_OF_MONTH, maxDay)
            targetCal.set(Calendar.HOUR_OF_DAY, 23)
            targetCal.set(Calendar.MINUTE, 59)
            targetCal.set(Calendar.SECOND, 59)
            val endMs = targetCal.timeInMillis

            val monthLabel = sdf.format(Date(startMs))

            val monthPayments = payments.filter { it.paymentDate in startMs..endMs }
            var interestIncome = 0.0
            var interestExpense = 0.0
            var collections = 0.0

            val loansMap = loans.associateBy { it.loan.id }

            monthPayments.forEach { p ->
                val loanSummary = loansMap[p.loanId]
                val isLent = loanSummary?.loan?.loanType == LoanType.LENT

                if (p.paymentType != PaymentType.INITIAL_DISBURSEMENT) {
                    if (isLent) {
                        collections += p.amount
                        interestIncome += when (p.paymentType) {
                            PaymentType.INTEREST -> p.amount
                            PaymentType.PRINCIPAL_AND_INTEREST -> p.interestComponent
                            PaymentType.FULL_SETTLEMENT -> p.interestComponent
                            else -> 0.0
                        }
                    } else {
                        interestExpense += when (p.paymentType) {
                            PaymentType.INTEREST -> p.amount
                            PaymentType.PRINCIPAL_AND_INTEREST -> p.interestComponent
                            PaymentType.FULL_SETTLEMENT -> p.interestComponent
                            else -> 0.0
                        }
                    }
                }
            }

            points.add(
                MonthlyProfitPoint(
                    monthYear = monthLabel,
                    interestIncome = interestIncome,
                    interestExpense = interestExpense,
                    netProfit = interestIncome - interestExpense,
                    collections = collections
                )
            )
        }
        return points
    }

    /**
     * Customer Profile Summary Flow
     */
    val customerSummariesFlow: Flow<List<CustomerFinancialSummary>> = combine(
        allCustomers,
        calculatedLoansFlow
    ) { customers, calculatedLoans ->
        val loansByCustomer = calculatedLoans.groupBy { it.loan.personId }

        customers.map { customer ->
            val custLoans = loansByCustomer[customer.id] ?: emptyList()
            var totalLent = 0.0
            var totalBorrowed = 0.0
            var totalPrincipalPaid = 0.0
            var totalInterestPaid = 0.0
            var outstandingBalance = 0.0
            var activeCount = 0
            var completedCount = 0

            custLoans.forEach { l ->
                if (l.loan.loanType == LoanType.LENT) {
                    totalLent += l.loan.principalAmount
                } else {
                    totalBorrowed += l.loan.principalAmount
                }
                totalPrincipalPaid += l.totalPrincipalPaid
                totalInterestPaid += l.totalInterestPaid
                outstandingBalance += l.totalBalance

                if (l.loan.status == LoanStatus.COMPLETED) {
                    completedCount++
                } else {
                    activeCount++
                }
            }

            CustomerFinancialSummary(
                customer = customer,
                totalLent = totalLent,
                totalBorrowed = totalBorrowed,
                totalPrincipalPaid = totalPrincipalPaid,
                totalInterestPaid = totalInterestPaid,
                outstandingBalance = outstandingBalance,
                activeLoansCount = activeCount,
                completedLoansCount = completedCount,
                loans = custLoans
            )
        }
    }

    /**
     * Unified Transactions Flow
     */
    val unifiedTransactionsFlow: Flow<List<TransactionItem>> = combine(
        allPayments,
        allLoans
    ) { payments, loans ->
        val loansMap = loans.associateBy { it.id }

        payments.map { p ->
            val loan = loansMap[p.loanId]
            val isLent = loan?.loanType != LoanType.BORROWED

            val txType = when (p.paymentType) {
                PaymentType.INITIAL_DISBURSEMENT -> if (isLent) TransactionType.LOAN_GIVEN else TransactionType.LOAN_TAKEN
                PaymentType.PRINCIPAL -> if (isLent) TransactionType.PRINCIPAL_COLLECTED else TransactionType.PRINCIPAL_PAID
                PaymentType.INTEREST -> if (isLent) TransactionType.INTEREST_COLLECTED else TransactionType.INTEREST_PAID
                PaymentType.PRINCIPAL_AND_INTEREST, PaymentType.FULL_SETTLEMENT -> if (isLent) TransactionType.PRINCIPAL_COLLECTED else TransactionType.PRINCIPAL_PAID
            }

            TransactionItem(
                id = p.id,
                loanId = p.loanId,
                personId = p.personId,
                personName = p.personName,
                date = p.paymentDate,
                transactionType = txType,
                amount = p.amount,
                paymentMode = p.paymentMode,
                notes = p.notes,
                isLoanDisbursement = p.paymentType == PaymentType.INITIAL_DISBURSEMENT,
                loanType = loan?.loanType ?: LoanType.LENT
            )
        }
    }

    // Direct methods for Backup & Restore
    suspend fun getAllCustomersDirect() = customerDao.getAllCustomers()
    suspend fun getAllLoansDirect() = loanDao.getAllLoansDirect()
    suspend fun getAllPaymentsDirect() = paymentDao.getAllPaymentsDirect()

    suspend fun restoreDatabase(
        customers: List<Customer>,
        loans: List<Loan>,
        payments: List<Payment>
    ) {
        paymentDao.clearAll()
        loanDao.clearAll()
        customerDao.clearAll()

        customerDao.insertAll(customers)
        loanDao.insertAll(loans)
        paymentDao.insertAll(payments)

        loans.forEach { recalculateLoanStatus(it.id) }
    }
}
