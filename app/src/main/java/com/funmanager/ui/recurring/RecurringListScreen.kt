package com.funmanager.ui.recurring

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.Category
import com.funmanager.data.RecurringRule
import com.funmanager.data.TransactionType
import com.funmanager.ui.components.AppFab
import com.funmanager.ui.components.CategoryIcon
import com.funmanager.ui.components.GlassCard
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.components.shortDate
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.FunManagerTheme
import com.funmanager.ui.theme.IncomeColor
import java.time.YearMonth

@Composable
fun RecurringListRoute(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: RecurringListViewModel = viewModel(factory = RecurringListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecurringListScreen(state, onBack, onAdd, onOpen, viewModel::setActive)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    state: RecurringListUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onToggle: (RecurringRule, Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            AppFab(onClick = onAdd, modifier = Modifier.navigationBarsPadding(), contentDescription = stringResource(R.string.add_recurring))
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.recurring_explain), style = MaterialTheme.typography.bodySmall)
                        Row {
                            Text(stringResource(R.string.recurring_monthly_expense), modifier = Modifier.weight(1f))
                            MoneyText(state.monthlyExpense, color = ExpenseColor)
                        }
                        Row {
                            Text(stringResource(R.string.recurring_monthly_income), modifier = Modifier.weight(1f))
                            MoneyText(state.monthlyIncome, color = IncomeColor)
                        }
                    }
                }
            }
            if (!state.loading && state.items.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.recurring_empty),
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.items.isNotEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            state.items.forEach { item ->
                                RecurringRow(item, onClick = { onOpen(item.rule.id) }, onToggle = { onToggle(item.rule, it) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringRow(item: RecurringItem, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).alpha(if (item.rule.active) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIcon(item.category)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.rule.note.ifBlank { item.category.name },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (item.rule.active) {
                        stringResource(R.string.recurring_schedule, item.rule.dayOfMonth, item.nextDate.shortDate())
                    } else {
                        stringResource(R.string.recurring_paused, item.rule.dayOfMonth)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(item.rule.amount, type = item.rule.type, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Switch(checked = item.rule.active, onCheckedChange = onToggle)
    }
}

@Preview(showBackground = true)
@Composable
private fun RecurringListPreview() {
    val home = Category(3, "Nhà ở", "home", 0xFF8D6E63, TransactionType.EXPENSE)
    val salary = Category(10, "Lương", "salary", 0xFF66BB6A, TransactionType.INCOME)
    val next = YearMonth.of(2026, 12)
    FunManagerTheme {
        RecurringListScreen(
            state = RecurringListUiState(
                items = listOf(
                    RecurringItem(RecurringRule(1, 5_000_000, TransactionType.EXPENSE, 3, 1, "Tiền nhà", next), home),
                    RecurringItem(RecurringRule(2, 15_000_000, TransactionType.INCOME, 10, 5, "", next), salary),
                ),
                loading = false,
            ),
            onBack = {},
            onAdd = {},
            onOpen = {},
            onToggle = { _, _ -> },
        )
    }
}
