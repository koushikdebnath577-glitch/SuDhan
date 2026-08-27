package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId"), Index("status"), Index("loanType")]
)
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val personName: String,
    val personPhone: String,
    val loanType: LoanType = LoanType.LENT, // LENT (Money given) or BORROWED (Money borrowed)
    val principalAmount: Double,
    val startDate: Long, // timestamp ms
    val dueDate: Long? = null, // timestamp ms
    val interestRate: Double, // percentage, e.g. 2.0 (% per month or per year)
    val interestPeriod: InterestPeriod = InterestPeriod.MONTHLY,
    val interestType: InterestType = InterestType.SIMPLE,
    val repaymentFrequency: RepaymentFrequency = RepaymentFrequency.MONTHLY,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
