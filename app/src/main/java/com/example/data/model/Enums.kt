package com.example.data.model

enum class CustomerType {
    CUSTOMER, // Borrower (we lent money to them)
    LENDER,   // Lender (we borrowed money from them)
    BOTH
}

enum class LoanType {
    LENT,     // Money Given / Given to Customer
    BORROWED  // Money Borrowed / Taken from Lender
}

enum class InterestPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class InterestType {
    SIMPLE,
    COMPOUND,
    FLAT,
    REDUCING_BALANCE
}

enum class RepaymentFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM
}

enum class LoanStatus {
    ACTIVE,
    COMPLETED,
    OVERDUE
}

enum class PaymentType {
    PRINCIPAL,
    INTEREST,
    PRINCIPAL_AND_INTEREST,
    FULL_SETTLEMENT,
    INITIAL_DISBURSEMENT
}

enum class PaymentMode {
    CASH,
    UPI,
    BANK_TRANSFER,
    OTHER
}

enum class AppLanguage {
    ENGLISH,
    BENGALI
}

enum class AppThemeSetting {
    SYSTEM,
    LIGHT,
    DARK
}
