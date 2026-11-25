package com.funmanager.lock

import com.funmanager.data.LockStore
import com.funmanager.domain.LockTimeout
import com.funmanager.domain.MAX_FAILED_ATTEMPTS
import com.funmanager.domain.hashPin
import com.funmanager.domain.isValidPin
import com.funmanager.domain.lockoutRemaining
import com.funmanager.domain.pinMatches
import com.funmanager.domain.shouldLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface UnlockResult {
    data object Success : UnlockResult
    data class Wrong(val attemptsLeft: Int) : UnlockResult
    data class LockedOut(val remainingMs: Long) : UnlockResult
}

data class LockSettings(
    val enabled: Boolean,
    val biometric: Boolean,
    val timeout: LockTimeout,
    val pinLength: Int,
)

/** Giữ trạng thái khoá của app và các thao tác đặt/kiểm tra PIN. */
class AppLockManager(private val store: LockStore, private val clock: () -> Long = System::currentTimeMillis) {

    private val _locked = MutableStateFlow(store.enabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<LockSettings> = _settings.asStateFlow()

    private var backgroundAt: Long? = null

    /** true khi hộp thoại vân tay của hệ thống đang mở: không coi là rời app. */
    var authenticating: Boolean = false

    fun onAppStopped() {
        if (authenticating) return
        backgroundAt = clock()
    }

    fun onAppStarted() {
        val wentAway = backgroundAt
        if (wentAway != null && shouldLock(store.enabled, wentAway, clock(), store.timeout)) {
            _locked.value = true
        }
        backgroundAt = null
    }

    fun lockoutRemainingMs(): Long = lockoutRemaining(store.failedAttempts, store.lastFailedAt, clock())

    fun unlockWithPin(pin: String): UnlockResult {
        val remaining = lockoutRemainingMs()
        if (remaining > 0) return UnlockResult.LockedOut(remaining)
        val hash = store.pinHash ?: return UnlockResult.Success.also { _locked.value = false }
        if (pinMatches(pin, store.salt, hash)) {
            store.failedAttempts = 0
            _locked.value = false
            return UnlockResult.Success
        }
        // Sau khi hết thời gian chờ, cho thêm một lượt trọn vẹn.
        val failed = if (store.failedAttempts >= MAX_FAILED_ATTEMPTS) 1 else store.failedAttempts + 1
        store.failedAttempts = failed
        store.lastFailedAt = clock()
        val lockout = lockoutRemainingMs()
        return if (lockout > 0) UnlockResult.LockedOut(lockout) else UnlockResult.Wrong(MAX_FAILED_ATTEMPTS - failed)
    }

    /** Kiểm tra PIN hiện tại mà không thay đổi trạng thái khoá (dùng khi tắt khoá / đổi PIN). */
    fun verifyPin(pin: String): Boolean {
        val hash = store.pinHash ?: return true
        return pinMatches(pin, store.salt, hash)
    }

    fun unlockWithBiometric() {
        store.failedAttempts = 0
        _locked.value = false
    }

    fun setPin(pin: String) {
        require(isValidPin(pin))
        store.setPin({ salt -> hashPin(pin, salt) }, pin.length)
        refresh()
    }

    fun disable() {
        store.clear()
        _locked.value = false
        refresh()
    }

    fun setBiometric(enabled: Boolean) {
        store.biometric = enabled
        refresh()
    }

    fun setTimeout(timeout: LockTimeout) {
        store.timeout = timeout
        refresh()
    }

    private fun refresh() {
        _settings.value = readSettings()
    }

    private fun readSettings() = LockSettings(
        enabled = store.enabled,
        biometric = store.biometric,
        timeout = store.timeout,
        pinLength = store.pinLength,
    )
}
