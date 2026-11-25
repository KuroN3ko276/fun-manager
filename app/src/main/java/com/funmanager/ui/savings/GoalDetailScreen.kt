package com.funmanager.ui.savings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.GoalWithSaved
import com.funmanager.data.SavingsDeposit
import com.funmanager.data.SavingsGoal
import com.funmanager.data.TransactionType
import com.funmanager.domain.formatVnd
import com.funmanager.ui.components.AmountField
import com.funmanager.ui.components.GlassCard
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.components.shortDate
import com.funmanager.ui.theme.FunManagerTheme
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun GoalDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: GoalDetailViewModel = viewModel(factory = GoalDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GoalDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = { onEdit(viewModel.goalId) },
        onDeposit = { viewModel.openDialog(withdraw = false) },
        onWithdraw = { viewModel.openDialog(withdraw = true) },
        onDepositClick = viewModel::requestDeleteDeposit,
    )
    state.dialog?.let { dialog ->
        MoveMoneyDialog(
            dialog = dialog,
            saved = state.item?.saved ?: 0,
            onAmount = viewModel::onDialogAmount,
            onNote = viewModel::onDialogNote,
            onDismiss = viewModel::dismissDialog,
            onConfirm = viewModel::confirmDialog,
        )
    }
    state.pendingDelete?.let { deposit ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDeposit,
            title = { Text(stringResource(R.string.delete_deposit_title)) },
            text = { Text(stringResource(R.string.delete_deposit_message, formatVnd(deposit.amount), deposit.date.shortDate())) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteDeposit) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDeposit) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    state: GoalDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onDepositClick: (SavingsDeposit) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.item?.goal?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, stringResource(R.string.edit_goal)) }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val item = state.item ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { GoalCard(item) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDeposit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.deposit))
                    }
                    OutlinedButton(onClick = onWithdraw, modifier = Modifier.weight(1f), enabled = item.saved > 0) {
                        Icon(Icons.Filled.Remove, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.withdraw))
                    }
                }
            }
            item { Text(stringResource(R.string.history), style = MaterialTheme.typography.titleSmall) }
            if (state.deposits.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.history_empty),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.deposits.isNotEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            state.deposits.forEach { deposit ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onDepositClick(deposit) }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(deposit.date.shortDate(), style = MaterialTheme.typography.bodyMedium)
                                        if (deposit.note.isNotBlank()) {
                                            Text(
                                                deposit.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    MoneyText(
                                        abs(deposit.amount),
                                        type = if (deposit.amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveMoneyDialog(
    dialog: MoveDialog,
    saved: Long,
    onAmount: (String) -> Unit,
    onNote: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (dialog.withdraw) R.string.withdraw else R.string.deposit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountField(dialog.amountText, onAmount, isError = dialog.error == AmountError.INVALID)
                if (dialog.error == AmountError.EXCEEDS_SAVED) {
                    Text(
                        stringResource(R.string.withdraw_too_much, formatVnd(saved)),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = dialog.note,
                    onValueChange = onNote,
                    label = { Text(stringResource(R.string.note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun GoalDetailPreview() {
    val day = LocalDate.of(2026, 11, 25)
    FunManagerTheme {
        GoalDetailScreen(
            state = GoalDetailUiState(
                item = GoalWithSaved(SavingsGoal(1, "Mua xe máy", 40_000_000, "transport", 0xFF42A5F5, day.plusMonths(7)), 12_500_000),
                deposits = listOf(
                    SavingsDeposit(2, 1, -500_000, day, "Sửa xe"),
                    SavingsDeposit(1, 1, 13_000_000, day.minusDays(20), "Lương tháng 10"),
                ),
                loading = false,
            ),
            onBack = {},
            onEdit = {},
            onDeposit = {},
            onWithdraw = {},
            onDepositClick = {},
        )
    }
}
