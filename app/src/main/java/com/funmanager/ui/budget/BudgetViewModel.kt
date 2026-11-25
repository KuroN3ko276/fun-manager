package com.funmanager.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.Budget
import com.funmanager.data.FinanceRepository
import com.funmanager.domain.BudgetOverview
import com.funmanager.domain.budgetOverview
import com.funmanager.domain.parseVnd
import com.funmanager.domain.summarize
import com.funmanager.ui.container
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BudgetUiState(
    val month: YearMonth = YearMonth.now(),
    val overview: BudgetOverview = BudgetOverview.EMPTY,
    val limits: Map<Long, Long> = emptyMap(),
    /** Tháng trước có ngân sách và tháng này chưa có → gợi ý sao chép. */
    val canCopyPrevious: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val repository: FinanceRepository,
    private val selectedMonth: MutableStateFlow<YearMonth>,
) : ViewModel() {

    val state: StateFlow<BudgetUiState> = selectedMonth.flatMapLatest { month ->
        combine(
            repository.transactionsIn(month),
            repository.budgetsIn(month),
            repository.budgetsIn(month.minusMonths(1)),
            repository.categories,
        ) { transactions, budgets, previousBudgets, categories ->
            BudgetUiState(
                month = month,
                overview = budgetOverview(summarize(transactions, categories), budgets, categories),
                limits = budgets.associate { it.categoryId to it.limit },
                canCopyPrevious = budgets.isEmpty() && previousBudgets.isNotEmpty(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun onMonthChange(month: YearMonth) {
        selectedMonth.value = month
    }

    /** [categoryId] = [Budget.TOTAL_CATEGORY_ID] cho ngân sách tổng; chuỗi rỗng để xoá. */
    fun setLimit(categoryId: Long, text: String) {
        val month = selectedMonth.value
        viewModelScope.launch { repository.setBudget(month, categoryId, parseVnd(text)) }
    }

    fun copyPrevious() {
        val month = selectedMonth.value
        viewModelScope.launch { repository.copyBudgets(month.minusMonths(1), month) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { BudgetViewModel(container.repository, container.selectedMonth) }
        }
    }
}
