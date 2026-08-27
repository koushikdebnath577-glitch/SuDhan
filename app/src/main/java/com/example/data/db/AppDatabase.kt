package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.dao.CustomerDao
import com.example.data.dao.LoanDao
import com.example.data.dao.PaymentDao
import com.example.data.model.Customer
import com.example.data.model.CustomerType
import com.example.data.model.InterestPeriod
import com.example.data.model.InterestType
import com.example.data.model.Loan
import com.example.data.model.LoanStatus
import com.example.data.model.LoanType
import com.example.data.model.Payment
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentType
import com.example.data.model.RepaymentFrequency

class Converters {
    @TypeConverter
    fun fromCustomerType(value: CustomerType): String = value.name
    @TypeConverter
    fun toCustomerType(value: String): CustomerType = try { CustomerType.valueOf(value) } catch (e: Exception) { CustomerType.CUSTOMER }

    @TypeConverter
    fun fromLoanType(value: LoanType): String = value.name
    @TypeConverter
    fun toLoanType(value: String): LoanType = try { LoanType.valueOf(value) } catch (e: Exception) { LoanType.LENT }

    @TypeConverter
    fun fromInterestPeriod(value: InterestPeriod): String = value.name
    @TypeConverter
    fun toInterestPeriod(value: String): InterestPeriod = try { InterestPeriod.valueOf(value) } catch (e: Exception) { InterestPeriod.MONTHLY }

    @TypeConverter
    fun fromInterestType(value: InterestType): String = value.name
    @TypeConverter
    fun toInterestType(value: String): InterestType = try { InterestType.valueOf(value) } catch (e: Exception) { InterestType.SIMPLE }

    @TypeConverter
    fun fromRepaymentFrequency(value: RepaymentFrequency): String = value.name
    @TypeConverter
    fun toRepaymentFrequency(value: String): RepaymentFrequency = try { RepaymentFrequency.valueOf(value) } catch (e: Exception) { RepaymentFrequency.MONTHLY }

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus): String = value.name
    @TypeConverter
    fun toLoanStatus(value: String): LoanStatus = try { LoanStatus.valueOf(value) } catch (e: Exception) { LoanStatus.ACTIVE }

    @TypeConverter
    fun fromPaymentType(value: PaymentType): String = value.name
    @TypeConverter
    fun toPaymentType(value: String): PaymentType = try { PaymentType.valueOf(value) } catch (e: Exception) { PaymentType.PRINCIPAL }

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode): String = value.name
    @TypeConverter
    fun toPaymentMode(value: String): PaymentMode = try { PaymentMode.valueOf(value) } catch (e: Exception) { PaymentMode.CASH }
}

@Database(
    entities = [Customer::class, Loan::class, Payment::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun loanDao(): LoanDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sudhan_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
