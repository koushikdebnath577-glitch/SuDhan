package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Loan
import com.example.data.model.LoanStatus
import com.example.data.model.LoanType
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    suspend fun getAllLoansDirect(): List<Loan>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    fun getLoanById(id: Long): Flow<Loan?>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanByIdDirect(id: Long): Loan?

    @Query("SELECT * FROM loans WHERE personId = :personId ORDER BY startDate DESC")
    fun getLoansByPerson(personId: Long): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE personId = :personId ORDER BY startDate DESC")
    suspend fun getLoansByPersonDirect(personId: Long): List<Loan>

    @Query("SELECT * FROM loans WHERE loanType = :type ORDER BY startDate DESC")
    fun getLoansByType(type: LoanType): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE status = :status ORDER BY startDate DESC")
    fun getLoansByStatus(status: LoanStatus): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE personName LIKE '%' || :query || '%' OR personPhone LIKE '%' || :query || '%' OR CAST(id AS TEXT) LIKE '%' || :query || '%'")
    fun searchLoans(query: String): Flow<List<Loan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Long)

    @Query("UPDATE loans SET status = :status WHERE id = :id")
    suspend fun updateLoanStatus(id: Long, status: LoanStatus)

    @Query("DELETE FROM loans")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loans: List<Loan>)
}
