package com.funmanager.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.funmanager.data.ReminderStore
import com.funmanager.domain.ReminderSettings
import com.funmanager.domain.nextReminderAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Bật/tắt, đổi giờ và hẹn báo thức cho lời nhắc buổi tối. */
class ReminderManager(context: Context, private val store: ReminderStore = ReminderStore(context)) {
    private val context = context.applicationContext

    private val _settings = MutableStateFlow(store.settings)
    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

    fun setEnabled(enabled: Boolean) = update(_settings.value.copy(enabled = enabled))

    fun setTime(time: LocalTime) = update(_settings.value.copy(time = time))

    private fun update(value: ReminderSettings) {
        store.settings = value
        _settings.value = value
        schedule()
    }

    /**
     * Hẹn lần nhắc kế tiếp (hoặc huỷ khi đã tắt). Dùng báo thức không chính xác (lệch tối đa vài phút)
     * nên không cần quyền SCHEDULE_EXACT_ALARM.
     */
    fun schedule() {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent()
        val s = store.settings
        if (!s.enabled) {
            alarms.cancel(pending)
            return
        }
        val at = nextReminderAt(LocalDateTime.now(), s.time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pending)
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val WINDOW_MS = 10 * 60 * 1000L
    }
}
