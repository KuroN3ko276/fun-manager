package com.funmanager.ui.report

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import com.funmanager.domain.CategoryTotal
import com.funmanager.domain.summarize
import com.funmanager.ui.components.CategoryIcon
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.components.MonthSelector
import com.funmanager.ui.components.PieChart
import com.funmanager.ui.components.PieChartPadding
import com.funmanager.ui.components.PieSlice
import com.funmanager.ui.components.uiColor
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.FunManagerTheme
import com.funmanager.ui.theme.IncomeColor
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ReportRoute(viewModel: ReportViewModel = viewModel(factory = ReportViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportScreen(state, viewModel::onMonthChange, viewModel::onTypeChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    state: ReportUiState,
    onMonthChange: (YearMonth) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_report)) }) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthSelector(state.month, onMonthChange) }
            item {
                val types = listOf(TransactionType.EXPENSE to R.string.expense, TransactionType.INCOME to R.string.income)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    types.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = state.type == type,
                            onClick = { onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            item {
                PieChart(
                    slices = state.totals.map { PieSlice(it.amount.toFloat(), it.category.uiColor) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = PieChartPadding * 2, vertical = 8.dp),
                ) {
                    Text(
                        stringResource(if (state.type == TransactionType.EXPENSE) R.string.total_expense else R.string.total_income),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    MoneyText(state.total, style = MaterialTheme.typography.titleLarge)
                    ChangeText(state)
                }
            }
            if (state.totals.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.empty_month),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.totals, key = { it.category.id }) { CategoryTotalRow(it) }
        }
    }
}

@Composable
private fun ChangeText(state: ReportUiState) {
    val change = state.change ?: return
    // Chi tăng là xấu (đỏ), thu tăng là tốt (xanh).
    val good = if (state.type == TransactionType.EXPENSE) change <= 0 else change >= 0
    val text = when {
        change > 0 -> stringResource(R.string.change_up, change)
        change < 0 -> stringResource(R.string.change_down, abs(change))
        else -> stringResource(R.string.change_same)
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (good) IncomeColor else ExpenseColor,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CategoryTotalRow(total: CategoryTotal) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryIcon(total.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(total.category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                MoneyText(total.amount)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    LinearProgressIndicator(
                        progress = { total.share },
                        color = total.category.uiColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp)),
                    )
                }
                Text(
                    "${(total.share * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportScreenPreview() {
    val food = Category(1, "Ăn uống", "food", 0xFFEF5350, TransactionType.EXPENSE)
    val bill = Category(2, "Hoá đơn", "bill", 0xFFFFA726, TransactionType.EXPENSE)
    val day = LocalDate.of(2026, 11, 1)
    val txs = listOf(
        Transaction(1, 2_500_000, TransactionType.EXPENSE, 1, day),
        Transaction(2, 1_200_000, TransactionType.EXPENSE, 2, day),
    )
    FunManagerTheme {
        ReportScreen(
            ReportUiState(
                month = YearMonth.of(2026, 11),
                summary = summarize(txs, listOf(food, bill)),
                previous = summarize(txs.take(1), listOf(food, bill)),
            ),
            onMonthChange = {},
            onTypeChange = {},
        )
    }
}
