package com.funmanager.data

import android.content.Context
import com.funmanager.ui.theme.Accent

/** Lưu màu chủ đạo người dùng chọn. */
class ThemeStore(context: Context) {
    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)

    var accent: Accent
        get() = Accent.fromName(prefs.getString(KEY_ACCENT, null))
        set(value) = prefs.edit().putString(KEY_ACCENT, value.name).apply()

    private companion object {
        const val KEY_ACCENT = "accent"
    }
}
