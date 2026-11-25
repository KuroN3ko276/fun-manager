package com.funmanager.data

import android.content.Context
import com.funmanager.domain.LockTimeout
import java.security.SecureRandom

/** Cài đặt khoá app, lưu trong SharedPreferences riêng của app (chỉ lưu bản băm của PIN). */
class LockStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    val enabled: Boolean get() = prefs.getString(KEY_HASH, null) != null

    val pinHash: String? get() = prefs.getString(KEY_HASH, null)
    val salt: String get() = prefs.getString(KEY_SALT, null) ?: ""
    val pinLength: Int get() = prefs.getInt(KEY_LENGTH, 0)

    var biometric: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    var timeout: LockTimeout
        get() = LockTimeout.entries.getOrNull(prefs.getInt(KEY_TIMEOUT, 0)) ?: LockTimeout.IMMEDIATELY
        set(value) = prefs.edit().putInt(KEY_TIMEOUT, value.ordinal).apply()

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED, value).apply()

    var lastFailedAt: Long
        get() = prefs.getLong(KEY_LAST_FAILED, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_FAILED, value).apply()

    /** Lưu PIN mới (đã băm) với salt ngẫu nhiên. */
    fun setPin(hash: (salt: String) -> String, length: Int) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hash(salt))
            .putInt(KEY_LENGTH, length)
            .putInt(KEY_FAILED, 0)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
        const val KEY_LENGTH = "pin_length"
        const val KEY_BIOMETRIC = "biometric"
        const val KEY_TIMEOUT = "timeout"
        const val KEY_FAILED = "failed_attempts"
        const val KEY_LAST_FAILED = "last_failed_at"
    }
}
