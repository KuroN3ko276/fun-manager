package com.funmanager.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.DefaultCategories
import com.funmanager.data.TransactionType
import com.funmanager.domain.DatePreset
import com.funmanager.ui.components.CategoryIcon
import com.funmanager.ui.components.DayGroupCard
import com.funmanager.ui.components.GlassCard
import com.funmanager.ui.components.IconBubble
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.components.iconFor
import com.funmanager.ui.components.shortDate
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.FunManagerTheme
import com.funmanager.ui.theme.IncomeColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        actions = SearchActions(
            onBack = onBack,
            onOpen = onOpen,
            onQueryChange = viewModel::onQueryChange,
            onToggleFilters = viewModel::toggleFilters,
            onTypeChange = viewModel::onTypeChange,
            onToggleCategory = viewModel::onToggleCategory,
            onToggleWallet = viewModel::onToggleWallet,
            onDatePreset = viewModel::onDatePreset,
            onCustomRange = viewModel::onCustomRange,
            onMinChange = viewModel::onMinChange,
            onMaxChange = viewModel::onMaxChange,
            onClearFilters = viewModel::clearFilters,
        ),
    )
}

class SearchActions(
    val onBack: () -> Unit = {},
    val onOpen: (Long) -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onToggleFilters: () -> Unit = {},
    val onTypeChange: (TransactionType?) -> Unit = {},
    val onToggleCategory: (Long) -> Unit = {},
    val onToggleWallet: (Long) -> Unit = {},
    val onDatePreset: (DatePreset) -> Unit = {},
    val onCustomRange: (LocalDate?, LocalDate?) -> Unit = { _, _ -> },
    val onMinChange: (String) -> Unit = {},
    val onMaxChange: (String) -> Unit = {},
    val onClearFilters: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(state: SearchUiState, actions: SearchActions, autoFocus: Boolean = true) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) focus.requestFocus() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.input.filter.query,
                        onValueChange = actions.onQueryChange,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.input.filter.query.isNotEmpty()) {
                                IconButton(onClick = { actions.onQueryChange("") }) {
                                    Icon(Icons.Filled.Clear, stringResource(R.string.clear))
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = actions.onToggleFilters) {
                        val count = state.input.filter.activeCount
                        BadgedBox(badge = { if (count > 0) Badge { Text("$count") } }) {
                            Icon(Icons.Filled.FilterList, stringResource(R.string.filters))
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.showFilters) {
                item { FilterPanel(state, actions) }
            }
            item { ResultSummary(state) }
            state.results.forEach { (date, items) ->
                item(key = "d" + date.toEpochDay()) {
                    DayGroupCard(date, items, actions.onOpen, extraInfo = { state.walletNames[it.transaction.walletId] })
                }
            }
        }
    }
}

@Composable
private fun ResultSummary(state: SearchUiState) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.search_count, state.count), style = MaterialTheme.typography.titleSmall)
            if (state.count > 0) {
                Row {
                    Text(stringResource(R.string.income) + ": ", style = MaterialTheme.typography.bodyMedium)
                    MoneyText(state.totalIncome, color = IncomeColor, style = MaterialTheme.typography.bodyMedium)
                }
                Row {
                    Text(stringResource(R.string.expense) + ": ", style = MaterialTheme.typography.bodyMedium)
                    MoneyText(state.totalExpense, color = ExpenseColor, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    stringResource(R.string.search_empty),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterPanel(state: SearchUiState, actions: SearchActions) {
    val filter = state.input.filter
    var showRangePicker by rememberSaveable { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.filter_type), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to R.string.all, TransactionType.EXPENSE to R.string.expense, TransactionType.INCOME to R.string.income)
                    .forEach { (type, label) ->
                        FilterChip(
                            selected = filter.type == type,
                            onClick = { actions.onTypeChange(type) },
                            label = { Text(stringResource(label)) },
                        )
                    }
            }

            HorizontalDivider()
            Text(stringResource(R.string.filter_time), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    DatePreset.ALL to R.string.preset_all,
                    DatePreset.THIS_MONTH to R.string.preset_this_month,
                    DatePreset.LAST_3_MONTHS to R.string.preset_3_months,
                    DatePreset.THIS_YEAR to R.string.preset_this_year,
                ).forEach { (preset, label) ->
                    FilterChip(
                        selected = filter.datePreset == preset,
                        onClick = { actions.onDatePreset(preset) },
                        label = { Text(stringResource(label)) },
                    )
                }
                FilterChip(
                    selected = filter.datePreset == DatePreset.CUSTOM,
                    onClick = { showRangePicker = true },
                    label = {
                        Text(
                            if (filter.datePreset == DatePreset.CUSTOM) {
                                "${filter.customFrom?.shortDate() ?: "…"} – ${filter.customTo?.shortDate() ?: "…"}"
                            } else {
                                stringResource(R.string.preset_custom)
                            },
                        )
                    },
                )
            }

            HorizontalDivider()
            Text(stringResource(R.string.filter_amount), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.input.minText,
                    onValueChange = actions.onMinChange,
                    label = { Text(stringResource(R.string.amount_from)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.input.maxText,
                    onValueChange = actions.onMaxChange,
                    label = { Text(stringResource(R.string.amount_to)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider()
            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories
                    .filter { filter.type == null || it.type == filter.type }
                    .forEach { category ->
                        FilterChip(
                            selected = category.id in filter.categoryIds,
                            onClick = { actions.onToggleCategory(category.id) },
                            label = { Text(category.name) },
                            leadingIcon = { CategoryIcon(category, size = 20.dp) },
                        )
                    }
            }

            if (state.wallets.size > 1) {
                HorizontalDivider()
                Text(stringResource(R.string.wallet), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.wallets.forEach { wallet ->
                        FilterChip(
                            selected = wallet.id in filter.walletIds,
                            onClick = { actions.onToggleWallet(wallet.id) },
                            label = { Text(wallet.name) },
                            leadingIcon = { IconBubble(iconFor(wallet.icon), Color(wallet.color), size = 20.dp) },
                        )
                    }
                }
            }

            if (filter.activeCount > 0) {
                TextButton(onClick = actions.onClearFilters) { Text(stringResource(R.string.clear_filters)) }
            }
        }
    }

    if (showRangePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = filter.customFrom?.toUtcMillis(),
            initialSelectedEndDateMillis = filter.customTo?.toUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    actions.onCustomRange(
                        pickerState.selectedStartDateMillis?.toLocalDate(),
                        pickerState.selectedEndDateMillis?.toLocalDate(),
                    )
                    showRangePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DateRangePicker(pickerState, modifier = Modifier.weight(1f))
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    FunManagerTheme {
        SearchScreen(
            state = SearchUiState(
                categories = DefaultCategories.all.mapIndexed { i, c -> c.copy(id = i + 1L) },
                showFilters = true,
            ),
            actions = SearchActions(),
            autoFocus = false,
        )
    }
}
