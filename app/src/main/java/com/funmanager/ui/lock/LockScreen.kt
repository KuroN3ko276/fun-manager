package com.funmanager.ui.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.funmanager.R
import com.funmanager.lock.UnlockResult
import com.funmanager.ui.theme.FunManagerTheme
import kotlinx.coroutines.delay

/**
 * Màn hình khoá che toàn bộ app.
 * [onSubmit] kiểm tra PIN khi đủ [pinLength] chữ số; [onBiometric] null nếu không dùng vân tay.
 */
@Composable
fun LockScreen(
    pinLength: Int,
    initialLockoutMs: Long,
    onSubmit: (String) -> UnlockResult,
    onBiometric: (() -> Unit)?,
    onExit: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var lockoutMs by rememberSaveable { mutableLongStateOf(initialLockoutMs) }
    val wrongText = stringResource(R.string.pin_wrong)

    BackHandler(onBack = onExit)
    LaunchedEffect(Unit) { onBiometric?.invoke() }
    LaunchedEffect(lockoutMs) {
        if (lockoutMs > 0) {
            delay(1_000)
            lockoutMs = (lockoutMs - 1_000).coerceAtLeast(0)
        }
    }

    fun submit(value: String) {
        when (val result = onSubmit(value)) {
            UnlockResult.Success -> Unit
            is UnlockResult.Wrong -> message = wrongText.format(result.attemptsLeft)
            is UnlockResult.LockedOut -> {
                message = null
                lockoutMs = result.remainingMs
            }
        }
        pin = ""
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.enter_pin), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))
            PinDots(pin.length, pinLength, isError = message != null && pin.isEmpty())
            Spacer(Modifier.height(12.dp))
            val info = when {
                lockoutMs > 0 -> stringResource(R.string.pin_locked_out, (lockoutMs + 999) / 1000)
                else -> message
            }
            Text(
                info.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(40.dp),
            )
            Spacer(Modifier.height(16.dp))
            PinPad(
                onDigit = { d ->
                    message = null
                    pin = appendDigit(pin, d, pinLength)
                    if (pin.length == pinLength) submit(pin)
                },
                onBackspace = { pin = pin.dropLast(1) },
                onBiometric = onBiometric,
                enabled = lockoutMs == 0L,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockScreenPreview() {
    FunManagerTheme {
        LockScreen(pinLength = 4, initialLockoutMs = 0, onSubmit = { UnlockResult.Wrong(3) }, onBiometric = {}, onExit = {})
    }
}
