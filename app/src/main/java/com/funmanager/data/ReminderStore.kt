package com.funmanager.data

import android.content.Context
import com.funmanager.domain.ReminderSettings
import java.time.LocalTime

/** Cài đặt nhắc ghi chi tiêu buổi tối, lưu trong SharedPreferences. */
class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("reminder", Context.MODE_PRIVATE)

    var settings: ReminderSettings
        get() = ReminderSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            time = LocalTime.ofSecondOfDay(prefs.getInt(KEY_MINUTE, ReminderSettings.DEFAULT_TIME.toSecondOfDay() / 60) * 60L),
        )
        set(value) = prefs.edit()
            .putBoolean(KEY_ENABLED, value.enabled)
            .putInt(KEY_MINUTE, value.time.toSecondOfDay() / 60)
            .apply()

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_MINUTE = "minute_of_day"
    }
}
