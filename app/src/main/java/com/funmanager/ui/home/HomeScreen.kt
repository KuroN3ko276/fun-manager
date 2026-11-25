package com.funmanager.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import com.funmanager.domain.summarize
import com.funmanager.ui.components.CategoryIcon
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.components.MonthSelector
import com.funmanager.ui.components.dayHeader
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.FunManagerTheme
import com.funmanager.ui.theme.IncomeColor
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeRoute(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel::onMonthChange, onAdd, onOpen)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onMonthChange: (YearMonth) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_transaction))
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { MonthSelector(state.month, onMonthChange) }
            item { SummaryCard(state) }
            if (!state.loading && state.days.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.empty_month),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            state.days.forEach { (date, items) ->
                item(key = date.toEpochDay()) { DayHeader(date, items) }
                items(items, key = { it.transaction.id }) { item ->
                    TransactionRow(item, onClick = { onOpen(item.transaction.id) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.balance), style = MaterialTheme.typography.labelLarge)
            MoneyText(state.summary.balance, style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.income), style = MaterialTheme.typography.labelMedium)
                    MoneyText(state.summary.income, color = IncomeColor)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.expense), style = MaterialTheme.typography.labelMedium)
                    MoneyText(state.summary.expense, color = ExpenseColor)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, items: List<TransactionItem>) {
    val net = items.sumOf {
        if (it.transaction.type == TransactionType.INCOME) it.transaction.amount else -it.transaction.amount
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(date.dayHeader(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        MoneyText(net, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TransactionRow(item: TransactionItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(item.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.category.name, style = MaterialTheme.typography.bodyLarge)
            if (item.transaction.note.isNotBlank()) {
                Text(
                    item.transaction.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MoneyText(item.transaction.amount, type = item.transaction.type)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val food = Category(1, "Ăn uống", "food", 0xFFEF5350, TransactionType.EXPENSE)
    val salary = Category(2, "Lương", "salary", 0xFF66BB6A, TransactionType.INCOME)
    val day = LocalDate.of(2026, 11, 25)
    val txs = listOf(
        Transaction(1, 15_000_000, TransactionType.INCOME, 2, day, "Lương tháng 11"),
        Transaction(2, 45_000, TransactionType.EXPENSE, 1, day, "Phở"),
    )
    val cats = listOf(food, salary)
    FunManagerTheme {
        HomeScreen(
            state = HomeUiState(
                month = YearMonth.of(2026, 11),
                summary = summarize(txs, cats),
                days = mapOf(day to txs.map { t -> TransactionItem(t, cats.first { it.id == t.categoryId }) }),
                loading = false,
            ),
            onMonthChange = {},
            onAdd = {},
            onOpen = {},
        )
    }
}
