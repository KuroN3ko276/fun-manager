package com.funmanager.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.Budget
import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import com.funmanager.domain.BudgetLevel
import com.funmanager.domain.BudgetProgress
import com.funmanager.domain.CategoryBudget
import com.funmanager.domain.budgetOverview
import com.funmanager.domain.formatVnd
import com.funmanager.domain.groupDigits
import com.funmanager.domain.summarize
import com.funmanager.ui.components.BudgetBar
import com.funmanager.ui.components.CategoryIcon
import com.funmanager.ui.components.GlassCard
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.components.MonthSelector
import com.funmanager.ui.components.color
import com.funmanager.ui.components.formatAmountInput
import com.funmanager.ui.components.label
import com.funmanager.ui.theme.FunManagerTheme
import java.time.YearMonth

@Composable
fun BudgetRoute(viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BudgetScreen(state, viewModel::onMonthChange, viewModel::setLimit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    state: BudgetUiState,
    onMonthChange: (YearMonth) -> Unit,
    onSetLimit: (categoryId: Long, text: String) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_budget)) }) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthSelector(state.month, onMonthChange) }
            item { TotalBudgetCard(state, onClick = { editing = Budget.TOTAL_CATEGORY_ID }) }
            item {
                Text(
                    stringResource(R.string.budget_carry_over_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(stringResource(R.string.budget_by_category), style = MaterialTheme.typography.titleSmall)
            }
            if (state.overview.categories.isNotEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            state.overview.categories.forEach { row ->
                                CategoryBudgetRow(row, inheritedSince(state, row.category.id), onClick = { editing = row.category.id })
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { categoryId ->
        val name = if (categoryId == Budget.TOTAL_CATEGORY_ID) {
            stringResource(R.string.total_budget)
        } else {
            state.overview.categories.firstOrNull { it.category.id == categoryId }?.category?.name.orEmpty()
        }
        LimitDialog(
            title = name,
            month = state.month,
            initial = state.limits[categoryId],
            onDismiss = { editing = null },
            onSave = { text ->
                onSetLimit(categoryId, text)
                editing = null
            },
        )
    }
}

@Composable
private fun TotalBudgetCard(state: BudgetUiState, onClick: () -> Unit) {
    val total = state.overview.total
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.total_budget), style = MaterialTheme.typography.labelLarge)
            if (total == null) {
                Text(stringResource(R.string.tap_to_set_budget), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.spent_amount, formatVnd(state.overview.totalSpent)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                ProgressDetails(total, large = true, since = inheritedSince(state, Budget.TOTAL_CATEGORY_ID))
            }
        }
    }
}

@Composable
private fun ProgressDetails(progress: BudgetProgress, large: Boolean = false, since: YearMonth? = null) {
    Row(verticalAlignment = Alignment.Bottom) {
        MoneyText(
            progress.spent,
            color = progress.level.color(),
            style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
        )
        Text(" / " + formatVnd(progress.limit), style = MaterialTheme.typography.bodyMedium)
    }
    BudgetBar(progress)
    val message = when (progress.level) {
        BudgetLevel.EXCEEDED -> stringResource(R.string.budget_exceeded, formatVnd(-progress.remaining))
        BudgetLevel.WARNING -> stringResource(R.string.budget_warning, formatVnd(progress.remaining))
        BudgetLevel.OK -> stringResource(R.string.budget_remaining, formatVnd(progress.remaining))
    }
    Text(message, style = MaterialTheme.typography.bodySmall, color = progress.level.color())
    if (since != null) {
        Text(
            stringResource(R.string.budget_since, since.label()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Tháng bắt đầu áp dụng nếu hạn mức được kế thừa từ tháng trước; null nếu đặt ngay trong tháng đang xem. */
private fun inheritedSince(state: BudgetUiState, categoryId: Long): YearMonth? =
    state.since[categoryId]?.takeIf { it < state.month }

@Composable
private fun CategoryBudgetRow(row: CategoryBudget, since: YearMonth?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(row.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.category.name, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            val progress = row.progress
            if (progress == null) {
                Text(
                    stringResource(R.string.spent_no_budget, formatVnd(row.spent)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ProgressDetails(progress, since = since)
            }
        }
    }
}

@Composable
private fun LimitDialog(title: String, month: YearMonth, initial: Long?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val initialText = initial?.let { groupDigits(it.toString()) }.orEmpty()
    var value by remember { mutableStateOf(TextFieldValue(initialText, selection = TextRange(initialText.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Savings, contentDescription = null) },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { v ->
                    val formatted = formatAmountInput(v.text)
                    value = TextFieldValue(formatted, selection = TextRange(formatted.length))
                },
                label = { Text(stringResource(R.string.monthly_limit)) },
                suffix = { Text("₫") },
                supportingText = { Text(stringResource(R.string.limit_hint_from, month.label())) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value.text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = { onSave("") }) {
                        Text(stringResource(R.string.remove_limit), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun BudgetScreenPreview() {
    val month = YearMonth.of(2026, 11)
    val food = Category(1, "Ăn uống", "food", 0xFFEF5350, TransactionType.EXPENSE)
    val entertainment = Category(2, "Giải trí", "entertainment", 0xFFEC407A, TransactionType.EXPENSE)
    val cats = listOf(food, entertainment)
    val txs = listOf(
        Transaction(1, 2_700_000, TransactionType.EXPENSE, 1, month.atDay(3)),
        Transaction(2, 400_000, TransactionType.EXPENSE, 2, month.atDay(4)),
    )
    val budgets = listOf(Budget(month, 0, 5_000_000), Budget(month, 1, 3_000_000))
    FunManagerTheme {
        BudgetScreen(
            BudgetUiState(
                month = month,
                overview = budgetOverview(summarize(txs, cats), budgets, cats),
                limits = budgets.associate { it.categoryId to it.limit },
            ),
            onMonthChange = {},
            onSetLimit = { _, _ -> },
        )
    }
}
