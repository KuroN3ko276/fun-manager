package com.funmanager.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.R
import com.funmanager.domain.LockTimeout
import com.funmanager.domain.PIN_MIN_LENGTH
import com.funmanager.domain.isValidPin
import com.funmanager.lock.AppLockManager
import com.funmanager.lock.LockSettings
import com.funmanager.ui.container
import com.funmanager.ui.lock.appendDigit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class PinPurpose { SET, CHANGE, DISABLE }

enum class PinStep { CURRENT, NEW, CONFIRM }

data class PinFlow(
    val purpose: PinPurpose,
    val step: PinStep,
    val entered: String = "",
    val firstPin: String = "",
    @StringRes val error: Int? = null,
)

data class SecurityUiState(
    val settings: LockSettings = LockSettings(enabled = false, biometric = false, timeout = LockTimeout.IMMEDIATELY, pinLength = 0),
    val biometricAvailable: Boolean = false,
    val pinFlow: PinFlow? = null,
)

class SecurityViewModel(private val lock: AppLockManager, biometricAvailable: Boolean) : ViewModel() {

    private val flow = MutableStateFlow<PinFlow?>(null)

    val state: StateFlow<SecurityUiState> = combine(lock.settings, flow) { settings, pinFlow ->
        SecurityUiState(settings, biometricAvailable, pinFlow)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SecurityUiState(biometricAvailable = biometricAvailable))

    fun onLockToggle(enable: Boolean) {
        flow.value = if (enable) {
            PinFlow(PinPurpose.SET, PinStep.NEW)
        } else {
            PinFlow(PinPurpose.DISABLE, PinStep.CURRENT)
        }
    }

    fun onChangePin() {
        flow.value = PinFlow(PinPurpose.CHANGE, PinStep.CURRENT)
    }

    fun onBiometricToggle(enable: Boolean) = lock.setBiometric(enable)

    fun onTimeoutChange(timeout: LockTimeout) = lock.setTimeout(timeout)

    fun dismissFlow() {
        flow.value = null
    }

    fun onDigit(d: Char) {
        val f = flow.value ?: return
        val entered = appendDigit(f.entered, d)
        flow.value = f.copy(entered = entered, error = null)
        // Khi nhập PIN hiện tại thì tự kiểm tra lúc đủ độ dài đã lưu.
        if (f.step == PinStep.CURRENT && entered.length == lock.settings.value.pinLength) onNext()
    }

    fun onBackspace() = flow.update { it?.copy(entered = it.entered.dropLast(1), error = null) }

    fun onNext() {
        val f = flow.value ?: return
        when (f.step) {
            PinStep.CURRENT -> when {
                !lock.verifyPin(f.entered) -> flow.value = f.copy(entered = "", error = R.string.pin_incorrect)
                f.purpose == PinPurpose.DISABLE -> {
                    lock.disable()
                    flow.value = null
                }
                else -> flow.value = PinFlow(f.purpose, PinStep.NEW)
            }
            PinStep.NEW ->
                flow.value = if (isValidPin(f.entered)) {
                    f.copy(step = PinStep.CONFIRM, firstPin = f.entered, entered = "")
                } else {
                    f.copy(error = R.string.pin_too_short)
                }
            PinStep.CONFIRM ->
                if (f.entered == f.firstPin) {
                    lock.setPin(f.entered)
                    flow.value = null
                } else {
                    flow.value = PinFlow(f.purpose, PinStep.NEW, error = R.string.pin_mismatch)
                }
        }
    }

    companion object {
        val MIN = PIN_MIN_LENGTH

        fun factory(biometricAvailable: Boolean) = viewModelFactory {
            initializer { SecurityViewModel(container.appLock, biometricAvailable) }
        }
    }
}
