package com.example.data.model

enum class TransactionType {
    LOAN_GIVEN,
    LOAN_TAKEN,
    PRINCIPAL_COLLECTED,
    INTEREST_COLLECTED,
    PRINCIPAL_PAID,
    INTEREST_PAID
}

data class LoanCalculatedSummary(
    val loan: Loan,
    val totalPrincipalPaid: Double,
    val totalInterestPaid: Double,
    val outstandingPrincipal: Double,
    val accruedInterest: Double,
    val outstandingInterest: Double,
    val totalPayable: Double,
    val totalBalance: Double,
    val isOverdue: Boolean,
    val nextPaymentDate: Long?,
    val payments: List<Payment> = emptyList()
)

data class CustomerFinancialSummary(
    val customer: Customer,
    val totalLent: Double,
    val totalBorrowed: Double,
    val totalPrincipalPaid: Double,
    val totalInterestPaid: Double,
    val outstandingBalance: Double,
    val activeLoansCount: Int,
    val completedLoansCount: Int,
    val loans: List<LoanCalculatedSummary> = emptyList()
)

data class DashboardSummary(
    val totalMoneyLent: Double = 0.0,
    val totalMoneyBorrowed: Double = 0.0,
    val totalPrincipalOutstanding: Double = 0.0,
    val totalInterestReceivable: Double = 0.0,
    val totalInterestPayable: Double = 0.0,
    val totalInterestEarned: Double = 0.0,
    val totalInterestPaid: Double = 0.0,
    val netInterestProfit: Double = 0.0,
    val activeLoansCount: Int = 0,
    val completedLoansCount: Int = 0,
    val overdueLoansCount: Int = 0,
    val todayCollection: Double = 0.0,
    val upcomingPaymentsCount: Int = 0,
    val monthlyProfits: List<MonthlyProfitPoint> = emptyList(),
    val upcomingLoans: List<LoanCalculatedSummary> = emptyList(),
    val overdueLoans: List<LoanCalculatedSummary> = emptyList()
)

data class MonthlyProfitPoint(
    val monthYear: String, // e.g. "Jan 2026"
    val interestIncome: Double,
    val interestExpense: Double,
    val netProfit: Double,
    val collections: Double
)

data class TransactionItem(
    val id: Long,
    val loanId: Long,
    val personId: Long,
    val personName: String,
    val date: Long,
    val transactionType: TransactionType,
    val amount: Double,
    val paymentMode: PaymentMode,
    val notes: String,
    val isLoanDisbursement: Boolean = false,
    val loanType: LoanType = LoanType.LENT
)
