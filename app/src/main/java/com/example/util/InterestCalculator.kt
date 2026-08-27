package com.example.util

import com.example.data.model.InterestPeriod
import com.example.data.model.InterestType
import com.example.data.model.Loan
import com.example.data.model.Payment
import com.example.data.model.PaymentType
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

object InterestCalculator {

    /**
     * Calculates days between two timestamps.
     */
    fun daysBetween(startTimeMs: Long, endTimeMs: Long): Double {
        val diff = endTimeMs - startTimeMs
        return max(0.0, diff.toDouble() / TimeUnit.DAYS.toMillis(1))
    }

    /**
     * Converts elapsed days into periods based on InterestPeriod.
     */
    fun daysToPeriods(days: Double, period: InterestPeriod): Double {
        return when (period) {
            InterestPeriod.DAILY -> days
            InterestPeriod.WEEKLY -> days / 7.0
            InterestPeriod.MONTHLY -> days / 30.0 // Standard commercial month (30 days)
            InterestPeriod.YEARLY -> days / 365.0
        }
    }

    /**
     * Calculate accrued interest on a principal for given rate and period up to target date.
     */
    fun calculateAccruedInterest(
        principal: Double,
        ratePercent: Double,
        period: InterestPeriod,
        type: InterestType,
        startDateMs: Long,
        targetDateMs: Long = System.currentTimeMillis()
    ): Double {
        if (principal <= 0 || ratePercent <= 0 || targetDateMs <= startDateMs) return 0.0

        val days = daysBetween(startDateMs, targetDateMs)
        val timeInPeriods = daysToPeriods(days, period)

        return when (type) {
            InterestType.SIMPLE -> {
                principal * (ratePercent / 100.0) * timeInPeriods
            }
            InterestType.COMPOUND -> {
                // Standard compound formula per period: A = P * (1 + r)^t
                val r = ratePercent / 100.0
                val amount = principal * (1.0 + r).pow(timeInPeriods)
                max(0.0, amount - principal)
            }
            InterestType.FLAT -> {
                // Flat rate applied over the duration
                principal * (ratePercent / 100.0) * timeInPeriods
            }
            InterestType.REDUCING_BALANCE -> {
                // Simple interest on currently remaining principal for elapsed time
                principal * (ratePercent / 100.0) * timeInPeriods
            }
        }
    }

    /**
     * Calculate loan lifecycle metrics considering all payments received/paid.
     */
    fun calculateLoanMetrics(
        loan: Loan,
        payments: List<Payment>,
        targetDateMs: Long = System.currentTimeMillis()
    ): LoanMetrics {
        val sortedPayments = payments.sortedBy { it.paymentDate }

        var principalPaidTotal = 0.0
        var interestPaidTotal = 0.0

        sortedPayments.forEach { p ->
            when (p.paymentType) {
                PaymentType.PRINCIPAL -> {
                    principalPaidTotal += p.amount
                }
                PaymentType.INTEREST -> {
                    interestPaidTotal += p.amount
                }
                PaymentType.PRINCIPAL_AND_INTEREST -> {
                    principalPaidTotal += p.principalComponent
                    interestPaidTotal += p.interestComponent
                }
                PaymentType.FULL_SETTLEMENT -> {
                    principalPaidTotal += p.principalComponent
                    interestPaidTotal += p.interestComponent
                }
                PaymentType.INITIAL_DISBURSEMENT -> {
                    // initial disbursement is not a repayment
                }
            }
        }

        val outstandingPrincipal = max(0.0, loan.principalAmount - principalPaidTotal)

        // Calculate interest accrued:
        // If reducing balance, calculate progressively across payments; otherwise on principal
        val totalAccruedInterest: Double = if (loan.interestType == InterestType.REDUCING_BALANCE) {
            calculateReducingBalanceInterestAccrued(loan, sortedPayments, targetDateMs)
        } else {
            calculateAccruedInterest(
                principal = loan.principalAmount,
                ratePercent = loan.interestRate,
                period = loan.interestPeriod,
                type = loan.interestType,
                startDateMs = loan.startDate,
                targetDateMs = targetDateMs
            )
        }

        val outstandingInterest = max(0.0, totalAccruedInterest - interestPaidTotal)
        val totalPayable = loan.principalAmount + totalAccruedInterest
        val totalBalance = outstandingPrincipal + outstandingInterest

        val isOverdue = loan.dueDate != null && targetDateMs > loan.dueDate && totalBalance > 1.0

        // Calculate next payment date
        val nextPaymentDate = calculateNextPaymentDate(loan, sortedPayments)

        return LoanMetrics(
            totalPrincipalPaid = principalPaidTotal,
            totalInterestPaid = interestPaidTotal,
            outstandingPrincipal = outstandingPrincipal,
            accruedInterest = totalAccruedInterest,
            outstandingInterest = outstandingInterest,
            totalPayable = totalPayable,
            totalBalance = totalBalance,
            isOverdue = isOverdue,
            nextPaymentDate = nextPaymentDate
        )
    }

    private fun calculateReducingBalanceInterestAccrued(
        loan: Loan,
        sortedPayments: List<Payment>,
        targetDateMs: Long
    ): Double {
        var currentPrincipal = loan.principalAmount
        var lastDate = loan.startDate
        var accruedSum = 0.0

        for (payment in sortedPayments) {
            if (payment.paymentDate > targetDateMs) break
            if (payment.paymentDate > lastDate && currentPrincipal > 0) {
                val days = daysBetween(lastDate, payment.paymentDate)
                val periods = daysToPeriods(days, loan.interestPeriod)
                accruedSum += currentPrincipal * (loan.interestRate / 100.0) * periods
            }

            val pAmount = when (payment.paymentType) {
                PaymentType.PRINCIPAL -> payment.amount
                PaymentType.PRINCIPAL_AND_INTEREST -> payment.principalComponent
                PaymentType.FULL_SETTLEMENT -> payment.principalComponent
                else -> 0.0
            }
            currentPrincipal = max(0.0, currentPrincipal - pAmount)
            lastDate = payment.paymentDate
        }

        if (targetDateMs > lastDate && currentPrincipal > 0) {
            val days = daysBetween(lastDate, targetDateMs)
            val periods = daysToPeriods(days, loan.interestPeriod)
            accruedSum += currentPrincipal * (loan.interestRate / 100.0) * periods
        }

        return accruedSum
    }

    private fun calculateNextPaymentDate(loan: Loan, payments: List<Payment>): Long? {
        if (loan.dueDate != null && loan.dueDate > System.currentTimeMillis()) {
            return loan.dueDate
        }
        val cal = Calendar.getInstance()
        val baseDate = if (payments.isNotEmpty()) payments.last().paymentDate else loan.startDate
        cal.timeInMillis = baseDate

        when (loan.repaymentFrequency) {
            com.example.data.model.RepaymentFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            com.example.data.model.RepaymentFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            com.example.data.model.RepaymentFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            com.example.data.model.RepaymentFrequency.CUSTOM -> {
                return loan.dueDate
            }
        }
        return cal.timeInMillis
    }

    /**
     * EMI calculation data class
     */
    data class EmiResult(
        val monthlyEmi: Double,
        val totalInterest: Double,
        val totalPayable: Double,
        val schedule: List<EmiScheduleItem>
    )

    data class EmiScheduleItem(
        val month: Int,
        val principal: Double,
        val interest: Double,
        val emi: Double,
        val balance: Double
    )

    /**
     * Calculate standard EMI
     * @param principal loan principal
     * @param annualRate annual interest percentage (e.g. 12.0 for 12% p.a.)
     * @param tenureMonths number of months
     */
    fun calculateEmi(principal: Double, annualRate: Double, tenureMonths: Int): EmiResult {
        if (principal <= 0 || tenureMonths <= 0) {
            return EmiResult(0.0, 0.0, 0.0, emptyList())
        }

        if (annualRate <= 0) {
            val emi = principal / tenureMonths
            val schedule = (1..tenureMonths).map { month ->
                EmiScheduleItem(
                    month = month,
                    principal = emi,
                    interest = 0.0,
                    emi = emi,
                    balance = max(0.0, principal - (emi * month))
                )
            }
            return EmiResult(emi, 0.0, principal, schedule)
        }

        val monthlyRate = (annualRate / 100.0) / 12.0
        val pow = (1.0 + monthlyRate).pow(tenureMonths.toDouble())
        val emi = principal * monthlyRate * pow / (pow - 1.0)
        val totalPayable = emi * tenureMonths
        val totalInterest = totalPayable - principal

        val schedule = mutableListOf<EmiScheduleItem>()
        var balance = principal

        for (m in 1..tenureMonths) {
            val interestComponent = balance * monthlyRate
            val principalComponent = emi - interestComponent
            balance = max(0.0, balance - principalComponent)
            schedule.add(
                EmiScheduleItem(
                    month = m,
                    principal = principalComponent,
                    interest = interestComponent,
                    emi = emi,
                    balance = balance
                )
            )
        }

        return EmiResult(
            monthlyEmi = emi,
            totalInterest = totalInterest,
            totalPayable = totalPayable,
            schedule = schedule
        )
    }

    /**
     * Direct simple interest computation for calculator
     */
    fun computeSimpleInterest(principal: Double, ratePercent: Double, timeYears: Double): Double {
        return principal * (ratePercent / 100.0) * timeYears
    }

    /**
     * Direct compound interest computation for calculator
     */
    fun computeCompoundInterest(
        principal: Double,
        ratePercent: Double,
        timeYears: Double,
        compoundingPerYear: Int = 12
    ): Double {
        val r = (ratePercent / 100.0) / compoundingPerYear
        val n = compoundingPerYear * timeYears
        val amount = principal * (1.0 + r).pow(n)
        return max(0.0, amount - principal)
    }

    /**
     * Daily interest computation
     */
    fun computeDailyInterest(principal: Double, dailyRatePercent: Double, days: Int): Double {
        return principal * (dailyRatePercent / 100.0) * days
    }

    /**
     * Monthly interest computation
     */
    fun computeMonthlyInterest(principal: Double, monthlyRatePercent: Double, months: Double): Double {
        return principal * (monthlyRatePercent / 100.0) * months
    }
}

data class LoanMetrics(
    val totalPrincipalPaid: Double,
    val totalInterestPaid: Double,
    val outstandingPrincipal: Double,
    val accruedInterest: Double,
    val outstandingInterest: Double,
    val totalPayable: Double,
    val totalBalance: Double,
    val isOverdue: Boolean,
    val nextPaymentDate: Long?
)
