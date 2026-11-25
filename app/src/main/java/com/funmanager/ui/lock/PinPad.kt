package com.funmanager.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.funmanager.R
import com.funmanager.domain.PIN_MAX_LENGTH

/** Các chấm thể hiện số chữ số đã nhập. */
@Composable
fun PinDots(entered: Int, total: Int, isError: Boolean = false) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(total.coerceAtLeast(entered)) { i ->
            Box(
                Modifier
                    .size(16.dp)
                    .then(
                        if (i < entered) Modifier.background(color, CircleShape) else Modifier.border(2.dp, color, CircleShape),
                    ),
            )
        }
    }
}

/** Bàn phím số 3×4; ô trái dưới là nút vân tay (nếu có). */
@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val rows = listOf("123", "456", "789")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { d -> DigitKey(d, onDigit, enabled) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(KEY_SIZE), contentAlignment = Alignment.Center) {
                if (onBiometric != null) {
                    IconButton(onClick = onBiometric, modifier = Modifier.size(KEY_SIZE)) {
                        Icon(
                            Icons.Filled.Fingerprint,
                            stringResource(R.string.use_fingerprint),
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            DigitKey('0', onDigit, enabled)
            IconButton(onClick = onBackspace, modifier = Modifier.size(KEY_SIZE), enabled = enabled) {
                Icon(Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.backspace))
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Char, onDigit: (Char) -> Unit, enabled: Boolean) {
    FilledTonalIconButton(onClick = { onDigit(digit) }, modifier = Modifier.size(KEY_SIZE), enabled = enabled) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineSmall, color = Color.Unspecified)
    }
}

private val KEY_SIZE = 72.dp

/** Thêm một chữ số nếu chưa đủ độ dài tối đa. */
fun appendDigit(current: String, digit: Char, max: Int = PIN_MAX_LENGTH): String =
    if (current.length < max) current + digit else current
