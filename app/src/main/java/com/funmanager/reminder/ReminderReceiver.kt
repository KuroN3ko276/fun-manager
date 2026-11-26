package com.funmanager.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.funmanager.FunManagerApp
import com.funmanager.widget.SummaryWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Nhận báo thức nhắc buổi tối; khởi động lại máy hoặc đổi giờ hệ thống thì hẹn lại. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as FunManagerApp).container
        if (intent.action != ACTION_REMIND) {
            container.reminders.schedule()
            return
        }
        val pending = goAsync()
        scope.launch {
            try {
                ReminderNotifier.notify(context, container.repository)
                // Sang buổi tối thì cũng làm mới widget (số "hôm nay còn được tiêu").
                SummaryWidgetProvider.updateAll(context)
            } finally {
                container.reminders.schedule()
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.funmanager.action.REMIND"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
