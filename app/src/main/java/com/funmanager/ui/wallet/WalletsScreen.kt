package com.funmanager.ui.wallet

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.Transfer
import com.funmanager.data.Wallet
import com.funmanager.domain.TransferError
import com.funmanager.domain.formatVnd
import com.funmanager.ui.components.AmountField
import com.funmanager.ui.components.AppFab
import com.funmanager.ui.components.CATEGORY_COLORS
import com.funmanager.ui.components.GlassCard
import com.funmanager.ui.components.HeroCard
import com.funmanager.ui.components.IconBubble
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.components.WALLET_ICONS
import com.funmanager.ui.components.WalletPicker
import com.funmanager.ui.components.iconFor
import com.funmanager.ui.components.onAccentGradient
import com.funmanager.ui.components.shortDate
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.FunManagerTheme
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun WalletsRoute(onBack: () -> Unit, viewModel: WalletsViewModel = viewModel(factory = WalletsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WalletsScreen(
        state = state,
        onBack = onBack,
        onAdd = viewModel::startCreate,
        onEdit = viewModel::startEdit,
        onTransfer = viewModel::startTransfer,
        onTransferClick = viewModel::requestDeleteTransfer,
    )
    state.editor?.let { editor ->
        WalletEditorDialog(
            editor = editor,
            onChange = viewModel::onEditorChange,
            onInitialChange = viewModel::onInitialChange,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveEditor,
            onDelete = viewModel::requestDelete,
        )
    }
    state.deleteRequest?.let { request ->
        DeleteWalletDialog(request, viewModel::onReplacementChange, viewModel::dismissDelete, viewModel::confirmDelete)
    }
    state.transferDialog?.let { dialog ->
        TransferMoneyDialog(
            dialog = dialog,
            wallets = state.wallets.map { it.wallet },
            onChange = viewModel::onTransferChange,
            onAmount = viewModel::onTransferAmount,
            onDismiss = viewModel::dismissTransfer,
            onConfirm = viewModel::confirmTransfer,
        )
    }
    state.pendingTransferDelete?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteTransfer,
            title = { Text(stringResource(R.string.delete_transfer_title)) },
            text = {
                Text(
                    "${item.from?.name} → ${item.to?.name}: ${formatVnd(item.transfer.amount)} (${item.transfer.date.shortDate()})",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteTransfer) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDeleteTransfer) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(
    state: WalletsUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Wallet) -> Unit,
    onTransfer: () -> Unit,
    onTransferClick: (TransferItem) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wallets)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
        floatingActionButton = {
            AppFab(onClick = onAdd, modifier = Modifier.navigationBarsPadding(), contentDescription = stringResource(R.string.add_wallet))
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                HeroCard {
                    val on = onAccentGradient()
                    Text(stringResource(R.string.total_assets), style = MaterialTheme.typography.titleSmall, color = on.copy(alpha = 0.85f))
                    MoneyText(state.total, color = on, style = MaterialTheme.typography.headlineLarge)
                    UpcomingText(state.totalUpcoming, color = on.copy(alpha = 0.85f))
                    if (state.wallets.size > 1) {
                        FilledTonalButton(
                            onClick = onTransfer,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = on.copy(alpha = 0.18f), contentColor = on),
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.transfer))
                        }
                    }
                }
            }
            if (state.wallets.isNotEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            state.wallets.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onEdit(item.wallet) }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconBubble(iconFor(item.wallet.icon), Color(item.wallet.color))
                                    Spacer(Modifier.width(12.dp))
                                    Text(item.wallet.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Column(horizontalAlignment = Alignment.End) {
                                        MoneyText(item.balance, color = if (item.balance < 0) ExpenseColor else Color.Unspecified)
                                        UpcomingText(item.upcoming)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (state.transfers.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.transfer_history),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                if (state.transfers.isNotEmpty()) {
                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                state.transfers.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { onTransferClick(item) }.padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(item.from?.name.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(horizontal = 4.dp),
                                                )
                                                Text(item.to?.name.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text(
                                                listOfNotNull(item.transfer.date.shortDate(), item.transfer.note.takeIf { it.isNotBlank() })
                                                    .joinToString(" · "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        MoneyText(item.transfer.amount)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Sắp tới: +X ₫" cho các khoản ghi ngày sau hôm nay (chưa tính vào số dư). */
@Composable
private fun UpcomingText(amount: Long, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    if (amount == 0L) return
    val sign = if (amount > 0) "+" else "−"
    Text(
        stringResource(R.string.upcoming_amount, sign + formatVnd(abs(amount))),
        style = MaterialTheme.typography.bodySmall,
        color = color,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletEditorDialog(
    editor: WalletEditor,
    onChange: ((WalletEditor) -> WalletEditor) -> Unit,
    onInitialChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editor.original == null) R.string.add_wallet else R.string.edit_wallet)) },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    label = { Text(stringResource(R.string.wallet_name)) },
                    placeholder = { Text(stringResource(R.string.wallet_name_hint)) },
                    isError = editor.nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountField(
                    editor.initialText,
                    onInitialChange,
                    isError = false,
                    label = stringResource(R.string.initial_balance),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.initial_is_debt), modifier = Modifier.weight(1f))
                    Switch(checked = editor.initialNegative, onCheckedChange = { v -> onChange { it.copy(initialNegative = v) } })
                }
                Text(stringResource(R.string.icon), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WALLET_ICONS.forEach { (key, icon) ->
                        val selected = key == editor.icon
                        IconBubble(
                            icon = icon,
                            color = if (selected) Color(editor.color) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .clickable { onChange { it.copy(icon = key) } }
                                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                        )
                    }
                }
                Text(stringResource(R.string.color), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORY_COLORS.forEach { color ->
                        IconBubble(
                            icon = Icons.Filled.Check,
                            color = Color(color),
                            size = 36.dp,
                            iconTint = if (color == editor.color) Color.White else Color.Transparent,
                            modifier = Modifier.clickable { onChange { it.copy(color = color) } },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                if (editor.original != null && editor.canDelete) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun DeleteWalletDialog(
    request: WalletDeleteRequest,
    onReplacementChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_wallet_title, request.wallet.name)) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.delete_wallet_merge, request.usage))
                request.replacements.forEach { w ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = request.replacementId == w.id,
                                onClick = { onReplacementChange(w.id) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = request.replacementId == w.id, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        IconBubble(iconFor(w.icon), Color(w.color), size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(w.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun TransferMoneyDialog(
    dialog: TransferDialog,
    wallets: List<Wallet>,
    onChange: ((TransferDialog) -> TransferDialog) -> Unit,
    onAmount: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.transfer)) },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WalletPicker(wallets, dialog.fromId, { id -> onChange { it.copy(fromId = id) } }, stringResource(R.string.transfer_from))
                WalletPicker(wallets, dialog.toId, { id -> onChange { it.copy(toId = id) } }, stringResource(R.string.transfer_to))
                if (dialog.error == TransferError.SAME_WALLET) {
                    Text(
                        stringResource(R.string.transfer_same_wallet),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                AmountField(dialog.amountText, onAmount, isError = dialog.error == TransferError.INVALID_AMOUNT)
                OutlinedTextField(
                    value = dialog.note,
                    onValueChange = { v -> onChange { it.copy(note = v) } },
                    label = { Text(stringResource(R.string.note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.transfer)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun WalletsPreview() {
    val cash = Wallet(1, "Tiền mặt", "cash", 0xFF66BB6A, 500_000)
    val bank = Wallet(2, "Vietcombank", "bank", 0xFF42A5F5, 10_000_000)
    val card = Wallet(3, "Thẻ tín dụng", "card", 0xFFAB47BC, 0)
    FunManagerTheme {
        WalletsScreen(
            state = WalletsUiState(
                wallets = listOf(
                    WalletWithBalance(cash, 1_250_000),
                    WalletWithBalance(bank, 23_400_000),
                    WalletWithBalance(card, -1_500_000),
                ),
                transfers = listOf(TransferItem(Transfer(1, 2, 1, 2_000_000, LocalDate.of(2026, 11, 20), "Rút ATM"), bank, cash)),
            ),
            onBack = {},
            onAdd = {},
            onEdit = {},
            onTransfer = {},
            onTransferClick = {},
        )
    }
}
