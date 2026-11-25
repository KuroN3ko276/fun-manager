package com.funmanager.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.FinanceRepository
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import com.funmanager.domain.MonthTotals
import com.funmanager.domain.MonthlySummary
import com.funmanager.domain.averageExpense
import com.funmanager.domain.monthlyTotals
import com.funmanager.domain.monthsEndingAt
import com.funmanager.domain.percentChange
import com.funmanager.domain.summarize
import com.funmanager.ui.container
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class ReportUiState(
    val month: YearMonth = YearMonth.now(),
    val type: TransactionType = TransactionType.EXPENSE,
    val summary: MonthlySummary = MonthlySummary.EMPTY,
    val previous: MonthlySummary = MonthlySummary.EMPTY,
    val trend: List<MonthTotals> = emptyList(),
) {
    val averageExpense: Long get() = averageExpense(trend)

    val total: Long get() = if (type == TransactionType.EXPENSE) summary.expense else summary.income
    private val previousTotal: Long get() = if (type == TransactionType.EXPENSE) previous.expense else previous.income
    val totals get() = if (type == TransactionType.EXPENSE) summary.expenseByCategory else summary.incomeByCategory

    /** % thay đổi so với tháng trước, null nếu tháng trước không có dữ liệu. */
    val change: Int? get() = percentChange(total, previousTotal)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    repository: FinanceRepository,
    private val selectedMonth: MutableStateFlow<YearMonth>,
) : ViewModel() {

    private val type = MutableStateFlow(TransactionType.EXPENSE)

    private val data = selectedMonth.flatMapLatest { month ->
        val months = monthsEndingAt(month, TREND_MONTHS)
        repository.transactionsBetween(months.first(), month).map { transactions ->
            MonthData(
                month = month,
                current = transactions.filter { YearMonth.from(it.date) == month },
                previous = transactions.filter { YearMonth.from(it.date) == month.minusMonths(1) },
                trend = monthlyTotals(transactions, months),
            )
        }
    }

    val state: StateFlow<ReportUiState> = combine(data, repository.categories, type) { d, categories, type ->
        ReportUiState(
            month = d.month,
            type = type,
            summary = summarize(d.current, categories),
            previous = summarize(d.previous, categories),
            trend = d.trend,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun onMonthChange(month: YearMonth) {
        selectedMonth.value = month
    }

    fun onTypeChange(value: TransactionType) {
        type.value = value
    }

    private data class MonthData(
        val month: YearMonth,
        val current: List<Transaction>,
        val previous: List<Transaction>,
        val trend: List<MonthTotals>,
    )

    companion object {
        const val TREND_MONTHS = 6

        val Factory = viewModelFactory {
            initializer { ReportViewModel(container.repository, container.selectedMonth) }
        }
    }
}
