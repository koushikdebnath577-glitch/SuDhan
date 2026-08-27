package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
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
import com.example.data.model.TransactionItem
import com.example.data.repository.LendingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun createJsonBackup(context: Context, repository: LendingRepository): File = withContext(Dispatchers.IO) {
        val customers = repository.allCustomers.first()
        val loans = repository.allLoans.first()
        val payments = repository.allPayments.first()

        val rootJson = JSONObject().apply {
            put("version", 1)
            put("appName", "SuDhan")
            put("timestamp", System.currentTimeMillis())
            put("backupDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            // Customers
            val customersArray = JSONArray()
            customers.forEach { c ->
                customersArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("address", c.address)
                    put("photoUri", c.photoUri ?: "")
                    put("type", c.type.name)
                    put("notes", c.notes)
                    put("createdAt", c.createdAt)
                })
            }
            put("customers", customersArray)

            // Loans
            val loansArray = JSONArray()
            loans.forEach { l ->
                loansArray.put(JSONObject().apply {
                    put("id", l.id)
                    put("personId", l.personId)
                    put("personName", l.personName)
                    put("personPhone", l.personPhone)
                    put("loanType", l.loanType.name)
                    put("principalAmount", l.principalAmount)
                    put("startDate", l.startDate)
                    put("dueDate", l.dueDate ?: -1L)
                    put("interestRate", l.interestRate)
                    put("interestPeriod", l.interestPeriod.name)
                    put("interestType", l.interestType.name)
                    put("repaymentFrequency", l.repaymentFrequency.name)
                    put("status", l.status.name)
                    put("notes", l.notes)
                    put("createdAt", l.createdAt)
                })
            }
            put("loans", loansArray)

            // Payments
            val paymentsArray = JSONArray()
            payments.forEach { p ->
                paymentsArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("loanId", p.loanId)
                    put("personId", p.personId)
                    put("personName", p.personName)
                    put("amount", p.amount)
                    put("paymentDate", p.paymentDate)
                    put("paymentType", p.paymentType.name)
                    put("principalComponent", p.principalComponent)
                    put("interestComponent", p.interestComponent)
                    put("paymentMode", p.paymentMode.name)
                    put("notes", p.notes)
                    put("createdAt", p.createdAt)
                })
            }
            put("payments", paymentsArray)
        }

        val cacheDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFile = File(cacheDir, "sudhan_backup_$dateStr.json")

        FileOutputStream(backupFile).use { fos ->
            fos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
        }

        backupFile
    }

    suspend fun restoreFromJson(jsonString: String, repository: LendingRepository): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val appName = root.optString("appName", "")
            if (appName != "SuDhan" && !root.has("customers")) {
                return@withContext Result.failure(Exception("Invalid backup file format"))
            }

            val customers = mutableListOf<Customer>()
            val customersArray = root.optJSONArray("customers") ?: JSONArray()
            for (i in 0 until customersArray.length()) {
                val obj = customersArray.getJSONObject(i)
                customers.add(
                    Customer(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", ""),
                        phone = obj.optString("phone", ""),
                        address = obj.optString("address", ""),
                        photoUri = obj.optString("photoUri").takeIf { it.isNotBlank() },
                        type = try { CustomerType.valueOf(obj.optString("type")) } catch (e: Exception) { CustomerType.CUSTOMER },
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val loans = mutableListOf<Loan>()
            val loansArray = root.optJSONArray("loans") ?: JSONArray()
            for (i in 0 until loansArray.length()) {
                val obj = loansArray.getJSONObject(i)
                val due = obj.optLong("dueDate", -1L)
                loans.add(
                    Loan(
                        id = obj.optLong("id", 0L),
                        personId = obj.optLong("personId", 0L),
                        personName = obj.optString("personName", ""),
                        personPhone = obj.optString("personPhone", ""),
                        loanType = try { LoanType.valueOf(obj.optString("loanType")) } catch (e: Exception) { LoanType.LENT },
                        principalAmount = obj.optDouble("principalAmount", 0.0),
                        startDate = obj.optLong("startDate", System.currentTimeMillis()),
                        dueDate = if (due > 0) due else null,
                        interestRate = obj.optDouble("interestRate", 0.0),
                        interestPeriod = try { InterestPeriod.valueOf(obj.optString("interestPeriod")) } catch (e: Exception) { InterestPeriod.MONTHLY },
                        interestType = try { InterestType.valueOf(obj.optString("interestType")) } catch (e: Exception) { InterestType.SIMPLE },
                        repaymentFrequency = try { RepaymentFrequency.valueOf(obj.optString("repaymentFrequency")) } catch (e: Exception) { RepaymentFrequency.MONTHLY },
                        status = try { LoanStatus.valueOf(obj.optString("status")) } catch (e: Exception) { LoanStatus.ACTIVE },
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val payments = mutableListOf<Payment>()
            val paymentsArray = root.optJSONArray("payments") ?: JSONArray()
            for (i in 0 until paymentsArray.length()) {
                val obj = paymentsArray.getJSONObject(i)
                payments.add(
                    Payment(
                        id = obj.optLong("id", 0L),
                        loanId = obj.optLong("loanId", 0L),
                        personId = obj.optLong("personId", 0L),
                        personName = obj.optString("personName", ""),
                        amount = obj.optDouble("amount", 0.0),
                        paymentDate = obj.optLong("paymentDate", System.currentTimeMillis()),
                        paymentType = try { PaymentType.valueOf(obj.optString("paymentType")) } catch (e: Exception) { PaymentType.PRINCIPAL },
                        principalComponent = obj.optDouble("principalComponent", 0.0),
                        interestComponent = obj.optDouble("interestComponent", 0.0),
                        paymentMode = try { PaymentMode.valueOf(obj.optString("paymentMode")) } catch (e: Exception) { PaymentMode.CASH },
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            repository.restoreDatabase(customers, loans, payments)
            Result.success("Restored ${customers.size} customers, ${loans.size} loans, and ${payments.size} payments successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportTransactionsCsv(context: Context, transactions: List<TransactionItem>): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val csvFile = File(cacheDir, "sudhan_transactions_$dateStr.csv")

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("Transaction ID,Date,Person Name,Type,Amount (INR),Payment Mode,Notes\n")

        transactions.forEach { t ->
            val date = sdf.format(Date(t.date))
            val cleanNotes = t.notes.replace(",", " ").replace("\n", " ")
            val cleanPerson = t.personName.replace(",", " ")
            sb.append("${t.id},\"$date\",\"$cleanPerson\",\"${t.transactionType}\",${t.amount},\"${t.paymentMode.name}\",\"$cleanNotes\"\n")
        }

        FileOutputStream(csvFile).use { fos ->
            fos.write(sb.toString().toByteArray(Charsets.UTF_8))
        }

        csvFile
    }

    suspend fun exportReportCsv(
        context: Context,
        reportTitle: String,
        headers: List<String>,
        rows: List<List<String>>
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanTitle = reportTitle.lowercase().replace(" ", "_").replace("&", "and")
        val csvFile = File(cacheDir, "sudhan_${cleanTitle}_$dateStr.csv")

        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { "\"$it\"" }).append("\n")

        rows.forEach { row ->
            sb.append(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }).append("\n")
        }

        FileOutputStream(csvFile).use { fos ->
            fos.write(sb.toString().toByteArray(Charsets.UTF_8))
        }

        csvFile
    }

    fun shareFile(context: Context, file: File, mimeType: String = "text/plain") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share SuDhan Data"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
