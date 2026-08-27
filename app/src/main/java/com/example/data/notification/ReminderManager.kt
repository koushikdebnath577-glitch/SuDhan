package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.model.LoanCalculatedSummary
import com.example.data.model.LoanType
import com.example.util.Formatters
import java.util.Calendar

data class ReminderItem(
    val id: Long,
    val title: String,
    val message: String,
    val personName: String,
    val amount: Double,
    val dueDate: Long,
    val loanType: LoanType,
    val reminderType: ReminderType
)

enum class ReminderType {
    DUE_TODAY,
    DUE_TOMORROW,
    OVERDUE,
    INTEREST_DUE
}

object ReminderManager {
    private const val CHANNEL_ID = "sudhan_reminders"
    private const val CHANNEL_NAME = "SuDhan Payment Reminders"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Loan & Interest payment reminders"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun generateReminders(loans: List<LoanCalculatedSummary>): List<ReminderItem> {
        val reminders = mutableListOf<ReminderItem>()
        val cal = Calendar.getInstance()

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val todayEnd = todayStart + 86_400_000L
        val tomorrowEnd = todayEnd + 86_400_000L

        loans.forEach { summary ->
            val loan = summary.loan
            if (summary.totalBalance > 1.0) {
                val due = loan.dueDate

                if (due != null) {
                    if (due < todayStart) {
                        // Overdue
                        reminders.add(
                            ReminderItem(
                                id = loan.id * 10 + 1,
                                title = "Overdue Payment Alert",
                                message = "Payment of ${Formatters.formatCurrency(summary.totalBalance)} from ${loan.personName} is OVERDUE!",
                                personName = loan.personName,
                                amount = summary.totalBalance,
                                dueDate = due,
                                loanType = loan.loanType,
                                reminderType = ReminderType.OVERDUE
                            )
                        )
                    } else if (due in todayStart..todayEnd) {
                        // Due Today
                        reminders.add(
                            ReminderItem(
                                id = loan.id * 10 + 2,
                                title = "Payment Due Today",
                                message = "${loan.personName} has a payment of ${Formatters.formatCurrency(summary.totalBalance)} due TODAY.",
                                personName = loan.personName,
                                amount = summary.totalBalance,
                                dueDate = due,
                                loanType = loan.loanType,
                                reminderType = ReminderType.DUE_TODAY
                            )
                        )
                    } else if (due in todayEnd..tomorrowEnd) {
                        // Due Tomorrow
                        reminders.add(
                            ReminderItem(
                                id = loan.id * 10 + 3,
                                title = "Payment Due Tomorrow",
                                message = "${loan.personName} has a payment of ${Formatters.formatCurrency(summary.totalBalance)} due tomorrow.",
                                personName = loan.personName,
                                amount = summary.totalBalance,
                                dueDate = due,
                                loanType = loan.loanType,
                                reminderType = ReminderType.DUE_TOMORROW
                            )
                        )
                    }
                }

                // Interest collection/payable reminder
                if (summary.outstandingInterest > 0) {
                    val isLent = loan.loanType == LoanType.LENT
                    reminders.add(
                        ReminderItem(
                            id = loan.id * 10 + 4,
                            title = if (isLent) "Interest Collection Due" else "Interest Payment Due",
                            message = if (isLent)
                                "Collect ${Formatters.formatCurrency(summary.outstandingInterest)} interest from ${loan.personName}"
                            else
                                "Pay ${Formatters.formatCurrency(summary.outstandingInterest)} interest to ${loan.personName}",
                            personName = loan.personName,
                            amount = summary.outstandingInterest,
                            dueDate = summary.nextPaymentDate ?: System.currentTimeMillis(),
                            loanType = loan.loanType,
                            reminderType = ReminderType.INTEREST_DUE
                        )
                    )
                }
            }
        }

        return reminders
    }

    fun showNotification(context: Context, reminder: ReminderItem) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(reminder.title)
                .setContentText(reminder.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "${reminder.message}\nPerson: ${reminder.personName}\nAmount: ${Formatters.formatCurrency(reminder.amount)}\nType: ${if (reminder.loanType == LoanType.LENT) "Lending Collection" else "Borrowing Repayment"}"
                ))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(reminder.id.toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
