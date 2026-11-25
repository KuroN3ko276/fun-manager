package com.funmanager.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.FinanceRepository
import com.funmanager.data.TransactionType
import com.funmanager.domain.MonthlySummary
import com.funmanager.domain.percentChange
import com.funmanager.domain.summarize
import com.funmanager.ui.container
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class ReportUiState(
    val month: YearMonth = YearMonth.now(),
    val type: TransactionType = TransactionType.EXPENSE,
    val summary: MonthlySummary = MonthlySummary.EMPTY,
    val previous: MonthlySummary = MonthlySummary.EMPTY,
) {
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

    private val months = selectedMonth.flatMapLatest { month ->
        combine(
            repository.transactionsIn(month),
            repository.transactionsIn(month.minusMonths(1)),
        ) { current, previous -> Triple(month, current, previous) }
    }

    val state: StateFlow<ReportUiState> = combine(months, repository.categories, type) { (month, current, previous), categories, type ->
        ReportUiState(
            month = month,
            type = type,
            summary = summarize(current, categories),
            previous = summarize(previous, categories),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun onMonthChange(month: YearMonth) {
        selectedMonth.value = month
    }

    fun onTypeChange(value: TransactionType) {
        type.value = value
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ReportViewModel(container.repository, container.selectedMonth) }
        }
    }
}
