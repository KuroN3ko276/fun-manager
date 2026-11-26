package com.funmanager.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.funmanager.MainActivity
import com.funmanager.R
import com.funmanager.data.FinanceRepository
import com.funmanager.data.TransactionType
import com.funmanager.domain.ReminderBody
import com.funmanager.domain.formatVnd
import com.funmanager.domain.reminderBody
import java.time.LocalDate

/** Thông báo buổi tối: hôm nay đã chi bao nhiêu và ngày mai được tiêu bao nhiêu. */
object ReminderNotifier {
    private const val CHANNEL_ID = "evening_reminder"
    private const val NOTIFICATION_ID = 21

    suspend fun notify(context: Context, repository: FinanceRepository, today: LocalDate = LocalDate.now()) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)

        val entries = repository.transactionsOn(today)
        val spentToday = entries.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val allowance = repository.dailyAllowance(today)
        val title = if (entries.isEmpty()) {
            context.getString(R.string.reminder_title_nothing)
        } else {
            context.getString(R.string.reminder_title_spent, formatVnd(spentToday))
        }
        val text = when (reminderBody(entries.size, allowance)) {
            ReminderBody.NOTHING_LOGGED -> context.getString(R.string.reminder_body_nothing)
            ReminderBody.TOMORROW_ALLOWANCE -> context.getString(R.string.reminder_body_tomorrow, formatVnd(allowance?.tomorrowPerDay ?: 0))
            ReminderBody.MONTH_OVER -> context.getString(R.string.reminder_body_over, formatVnd(allowance?.overBy ?: 0))
            ReminderBody.LAST_DAY -> context.getString(R.string.reminder_body_last_day)
            ReminderBody.NO_BUDGET -> context.getString(R.string.reminder_body_no_budget, entries.size)
        }

        // Màn hình khoá chỉ hiện lời nhắc chung, không lộ số tiền.
        val public = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_public_title))
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(activityIntent(context, add = false))
            .addAction(0, context.getString(R.string.reminder_action_add), activityIntent(context, add = true))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Quyền thông báo bị thu hồi giữa chừng: bỏ qua lần nhắc này.
        }
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun activityIntent(context: Context, add: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_ADD_TRANSACTION, add)
        return PendingIntent.getActivity(
            context,
            REQUEST_BASE + if (add) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val REQUEST_BASE = 2100
}
