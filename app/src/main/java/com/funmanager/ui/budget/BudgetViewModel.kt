package com.funmanager.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.Budget
import com.funmanager.data.BudgetAdjustment
import com.funmanager.data.FinanceRepository
import com.funmanager.domain.BudgetOverview
import com.funmanager.domain.applyAdjustments
import com.funmanager.domain.budgetOverview
import com.funmanager.domain.netAdjustments
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
    /** Hạn mức gốc (chưa tính chuyển trong tháng): giá trị ban đầu khi sửa hạn mức "từ tháng này trở đi". */
    val baseLimits: Map<Long, Long> = emptyMap(),
    /** Các lần chuyển hạn mức trong tháng đang xem. */
    val adjustments: List<BudgetAdjustment> = emptyList(),
    /** Chênh lệch hạn mức theo danh mục do các lần chuyển trong tháng. */
    val netAdjustment: Map<Long, Long> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val repository: FinanceRepository,
    private val selectedMonth: MutableStateFlow<YearMonth>,
) : ViewModel() {

    val state: StateFlow<BudgetUiState> = selectedMonth.flatMapLatest { month ->
        combine(
            repository.transactionsIn(month),
            repository.baseBudgetsIn(month),
            repository.adjustmentsIn(month),
            repository.categories,
        ) { transactions, base, adjustments, categories ->
            val budgets = applyAdjustments(base, adjustments, month)
            BudgetUiState(
                month = month,
                overview = budgetOverview(summarize(transactions, categories), budgets, categories),
                limits = budgets.associate { it.categoryId to it.limit },
                since = budgets.associate { it.categoryId to it.month },
                baseLimits = base.associate { it.categoryId to it.limit },
                adjustments = adjustments,
                netAdjustment = netAdjustments(adjustments),
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

    /** Chuyển hạn mức giữa hai danh mục, chỉ trong tháng đang xem. */
    fun reallocate(fromId: Long, toId: Long, amountText: String) {
        val month = selectedMonth.value
        val amount = parseVnd(amountText) ?: return
        viewModelScope.launch { repository.reallocate(month, fromId, toId, amount) }
    }

    fun undoAdjustment(id: Long) {
        viewModelScope.launch { repository.deleteAdjustment(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { BudgetViewModel(container.repository, container.selectedMonth) }
        }
    }
}
