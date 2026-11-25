package com.funmanager.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.DefaultCategories
import com.funmanager.data.TransactionType
import com.funmanager.ui.components.AmountField
import com.funmanager.ui.components.CategoryPicker
import com.funmanager.ui.components.DayOfMonthPicker
import com.funmanager.ui.components.TypeSegmented
import com.funmanager.ui.components.WalletPicker
import com.funmanager.ui.components.shortDate
import com.funmanager.ui.theme.FunManagerTheme
import java.time.LocalDate

@Composable
fun EditRecurringRoute(
    onDone: () -> Unit,
    viewModel: EditRecurringViewModel = viewModel(factory = EditRecurringViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    EditRecurringScreen(
        state = state,
        onBack = onDone,
        onTypeChange = viewModel::onTypeChange,
        onAmountChange = viewModel::onAmountChange,
        onCategoryChange = viewModel::onCategoryChange,
        onWalletChange = viewModel::onWalletChange,
        onDayChange = viewModel::onDayChange,
        onNoteChange = viewModel::onNoteChange,
        onActiveChange = viewModel::onActiveChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecurringScreen(
    state: EditRecurringUiState,
    onBack: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (Long) -> Unit,
    onWalletChange: (Long) -> Unit,
    onDayChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.add_recurring else R.string.edit_recurring)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TypeSegmented(state.type, onTypeChange)
            AmountField(state.amountText, onAmountChange, state.amountError)
            CategoryPicker(state.categories, state.categoryId, onCategoryChange, state.categoryError)
            if (state.wallets.size > 1) {
                WalletPicker(state.wallets, state.walletId, onWalletChange, stringResource(R.string.wallet))
            }

            Text(
                stringResource(R.string.day_of_month_selected, state.dayOfMonth),
                style = MaterialTheme.typography.titleSmall,
            )
            DayOfMonthPicker(state.dayOfMonth, onDayChange)
            if (state.dayOfMonth > 28) {
                Text(
                    stringResource(R.string.day_clamp_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.recurring_note)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recurring_active), modifier = Modifier.weight(1f))
                Switch(checked = state.active, onCheckedChange = onActiveChange)
            }

            Text(
                stringResource(R.string.recurring_next, state.nextDate(LocalDate.now()).shortDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_recurring_title)) },
            text = { Text(stringResource(R.string.delete_recurring_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditRecurringPreview() {
    FunManagerTheme {
        EditRecurringScreen(
            state = EditRecurringUiState(
                amountText = "5.000.000",
                categoryId = 3,
                dayOfMonth = 1,
                note = "Tiền nhà",
                allCategories = DefaultCategories.all.mapIndexed { i, c -> c.copy(id = i + 1L) },
            ),
            onBack = {},
            onTypeChange = {},
            onAmountChange = {},
            onCategoryChange = {},
            onWalletChange = {},
            onDayChange = {},
            onNoteChange = {},
            onActiveChange = {},
            onSave = {},
            onDelete = {},
        )
    }
}
