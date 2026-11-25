package com.funmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.funmanager.data.Wallet

/** Chọn một ví bằng các chip; [title] hiển thị phía trên nếu có. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletPicker(wallets: List<Wallet>, selectedId: Long?, onSelect: (Long) -> Unit, title: String? = null) {
    Column {
        if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            wallets.forEach { wallet ->
                FilterChip(
                    selected = wallet.id == selectedId,
                    onClick = { onSelect(wallet.id) },
                    label = { Text(wallet.name) },
                    leadingIcon = { IconBubble(iconFor(wallet.icon), Color(wallet.color), size = 20.dp) },
                )
            }
        }
    }
}

/** Tên ví theo id để hiển thị cạnh giao dịch; rỗng nếu chỉ có một ví. */
fun walletNames(wallets: List<Wallet>): Map<Long, String> =
    if (wallets.size > 1) wallets.associate { it.id to it.name } else emptyMap()
