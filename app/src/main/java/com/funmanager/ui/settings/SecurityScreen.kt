package com.funmanager.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.domain.LockTimeout
import com.funmanager.lock.Biometric
import com.funmanager.lock.LockSettings
import com.funmanager.ui.components.AppBackground
import com.funmanager.ui.components.GlassCard
import com.funmanager.ui.lock.PinDots
import com.funmanager.ui.lock.PinPad
import com.funmanager.ui.theme.FunManagerTheme

@Composable
fun SecurityRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SecurityViewModel = viewModel(factory = SecurityViewModel.factory(Biometric.isAvailable(context)))
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecurityScreen(
        state = state,
        onBack = onBack,
        onLockToggle = viewModel::onLockToggle,
        onChangePin = viewModel::onChangePin,
        onBiometricToggle = viewModel::onBiometricToggle,
        onTimeoutChange = viewModel::onTimeoutChange,
    )
    state.pinFlow?.let { flow ->
        PinFlowDialog(
            flow = flow,
            storedLength = state.settings.pinLength,
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            onNext = viewModel::onNext,
            onDismiss = viewModel::dismissFlow,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    state: SecurityUiState,
    onBack: () -> Unit,
    onLockToggle: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onTimeoutChange: (LockTimeout) -> Unit,
) {
    val s = state.settings
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    SettingSwitch(stringResource(R.string.lock_with_pin), stringResource(R.string.lock_with_pin_desc), s.enabled, onLockToggle)
                    if (s.enabled) {
                        HorizontalDivider()
                        Text(
                            stringResource(R.string.change_pin),
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onChangePin).padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (state.biometricAvailable) {
                            HorizontalDivider()
                            SettingSwitch(stringResource(R.string.unlock_fingerprint), null, s.biometric, onBiometricToggle)
                        }
                    }
                }
            }
            if (s.enabled) {
                Text(stringResource(R.string.lock_after), style = MaterialTheme.typography.titleSmall)
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        listOf(
                            LockTimeout.IMMEDIATELY to R.string.lock_immediately,
                            LockTimeout.ONE_MINUTE to R.string.lock_1_minute,
                            LockTimeout.FIVE_MINUTES to R.string.lock_5_minutes,
                        ).forEach { (timeout, label) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(s.timeout == timeout, onClick = { onTimeoutChange(timeout) }, role = Role.RadioButton)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = s.timeout == timeout, onClick = null)
                                Text(stringResource(label), modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.forgot_pin_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PinFlowDialog(
    flow: PinFlow,
    storedLength: Int,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AppBackground {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val title = when (flow.step) {
                    PinStep.CURRENT -> R.string.enter_current_pin
                    PinStep.NEW -> R.string.enter_new_pin
                    PinStep.CONFIRM -> R.string.confirm_new_pin
                }
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                val dots = when (flow.step) {
                    PinStep.CURRENT -> storedLength
                    PinStep.CONFIRM -> flow.firstPin.length
                    PinStep.NEW -> SecurityViewModel.MIN
                }
                PinDots(flow.entered.length, dots, isError = flow.error != null)
                Spacer(Modifier.height(12.dp))
                Text(
                    flow.error?.let { stringResource(it) } ?: if (flow.step == PinStep.NEW) stringResource(R.string.pin_length_hint) else "",
                    color = if (flow.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.height(40.dp),
                )
                Spacer(Modifier.height(16.dp))
                PinPad(onDigit = onDigit, onBackspace = onBackspace)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    if (flow.step != PinStep.CURRENT) {
                        Button(onClick = onNext, enabled = flow.entered.length >= SecurityViewModel.MIN) {
                            Text(stringResource(if (flow.step == PinStep.NEW) R.string.next else R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SecurityScreenPreview() {
    FunManagerTheme {
        SecurityScreen(
            state = SecurityUiState(
                settings = LockSettings(enabled = true, biometric = true, timeout = LockTimeout.ONE_MINUTE, pinLength = 4),
                biometricAvailable = true,
            ),
            onBack = {},
            onLockToggle = {},
            onChangePin = {},
            onBiometricToggle = {},
            onTimeoutChange = {},
        )
    }
}
