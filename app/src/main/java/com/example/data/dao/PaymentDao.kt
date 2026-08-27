package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    suspend fun getAllPaymentsDirect(): List<Payment>

    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDate ASC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDate ASC")
    suspend fun getPaymentsForLoanDirect(loanId: Long): List<Payment>

    @Query("SELECT * FROM payments WHERE personId = :personId ORDER BY paymentDate DESC")
    fun getPaymentsForPerson(personId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE paymentDate BETWEEN :startDate AND :endDate ORDER BY paymentDate DESC")
    fun getPaymentsBetween(startDate: Long, endDate: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE paymentDate BETWEEN :startDate AND :endDate ORDER BY paymentDate DESC")
    suspend fun getPaymentsBetweenDirect(startDate: Long, endDate: Long): List<Payment>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): Payment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM payments WHERE loanId = :loanId")
    suspend fun deletePaymentsForLoan(loanId: Long)

    @Query("DELETE FROM payments")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<Payment>)
}
