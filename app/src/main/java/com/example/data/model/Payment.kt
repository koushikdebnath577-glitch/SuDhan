package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("loanId"), Index("personId"), Index("paymentDate")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val personId: Long,
    val personName: String,
    val amount: Double,
    val paymentDate: Long, // timestamp ms
    val paymentType: PaymentType, // PRINCIPAL, INTEREST, PRINCIPAL_AND_INTEREST, FULL_SETTLEMENT, INITIAL_DISBURSEMENT
    val principalComponent: Double = 0.0,
    val interestComponent: Double = 0.0,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
