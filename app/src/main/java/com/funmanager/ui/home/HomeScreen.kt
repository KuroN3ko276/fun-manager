package com.funmanager.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import com.funmanager.domain.BudgetLevel
import com.funmanager.domain.BudgetOverview
import com.funmanager.domain.formatVnd
import com.funmanager.domain.summarize
import com.funmanager.ui.components.AppFab
import com.funmanager.ui.components.BalanceHero
import com.funmanager.ui.components.DayGroupCard
import com.funmanager.ui.components.GlassCard
import com.funmanager.ui.components.MonthSelector
import com.funmanager.ui.components.TransactionItem
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.FunManagerTheme
import com.funmanager.ui.theme.WarningColor
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun HomeRoute(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenWallets: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAppearance: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel::onMonthChange, onAdd, onOpen, onOpenBudget, onOpenCategories, onOpenRecurring, onOpenSearch, onOpenWallets, onOpenSecurity, onOpenAppearance)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onMonthChange: (YearMonth) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenWallets: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAppearance: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, stringResource(R.string.search))
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.wallets)) },
                            leadingIcon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenWallets()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_recurring)) },
                            leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenRecurring()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.categories)) },
                            leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenCategories()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.appearance)) },
                            leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenAppearance()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.security)) },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenSecurity()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            AppFab(onClick = onAdd, contentDescription = stringResource(R.string.add_transaction))
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthSelector(state.month, onMonthChange) }
            item { SummaryCard(state) }
            if (state.budget.hasAlert) {
                item { BudgetAlert(state.budget, onClick = onOpenBudget) }
            }
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
                item(key = date.toEpochDay()) {
                    DayGroupCard(date, items, onOpen, extraInfo = { state.walletNames[it.transaction.walletId] })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    BalanceHero(
        label = stringResource(R.string.balance_of_month),
        balance = state.summary.balance,
        income = state.summary.income,
        expense = state.summary.expense,
        incomeLabel = stringResource(R.string.income),
        expenseLabel = stringResource(R.string.expense),
    )
}

@Composable
private fun BudgetAlert(budget: BudgetOverview, onClick: () -> Unit) {
    val total = budget.total
    val exceeded = total?.level == BudgetLevel.EXCEEDED || budget.alerts.any { it.progress?.level == BudgetLevel.EXCEEDED }
    val tint = if (exceeded) ExpenseColor else WarningColor
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick, strong = true) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).background(tint.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                when (total?.level) {
                    BudgetLevel.EXCEEDED -> Text(
                        stringResource(R.string.alert_total_exceeded, formatVnd(-total.remaining)),
                        color = tint,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    BudgetLevel.WARNING -> Text(
                        stringResource(R.string.alert_total_warning, (total.ratio * 100).roundToInt()),
                        color = tint,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    else -> Unit
                }
                if (budget.alerts.isNotEmpty()) {
                    Text(
                        stringResource(R.string.alert_categories, budget.alerts.joinToString { it.category.name }),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
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
            onOpenBudget = {},
            onOpenCategories = {},
            onOpenRecurring = {},
            onOpenSearch = {},
            onOpenWallets = {},
            onOpenSecurity = {},
            onOpenAppearance = {},
        )
    }
}
