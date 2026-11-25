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
    /** Tháng bắt đầu áp dụng của từng hạn mức (để hiện "từ tháng X"). */
    val since: Map<Long, YearMonth> = emptyMap(),
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
            repository.categories,
        ) { transactions, budgets, categories ->
            BudgetUiState(
                month = month,
                overview = budgetOverview(summarize(transactions, categories), budgets, categories),
                limits = budgets.associate { it.categoryId to it.limit },
                since = budgets.associate { it.categoryId to it.month },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun onMonthChange(month: YearMonth) {
        selectedMonth.value = month
    }

    /** [categoryId] = [Budget.TOTAL_CATEGORY_ID] cho ngân sách tổng; chuỗi rỗng để bỏ từ tháng này. */
    fun setLimit(categoryId: Long, text: String) {
        val month = selectedMonth.value
        viewModelScope.launch { repository.setBudget(month, categoryId, parseVnd(text)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { BudgetViewModel(container.repository, container.selectedMonth) }
        }
    }
}
