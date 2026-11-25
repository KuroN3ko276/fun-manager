package com.funmanager.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.Category
import com.funmanager.data.FinanceRepository
import com.funmanager.data.RecurringRule
import com.funmanager.data.TransactionType
import com.funmanager.domain.occurrenceDate
import com.funmanager.ui.container
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RecurringItem(val rule: RecurringRule, val category: Category) {
    val nextDate: LocalDate get() = occurrenceDate(rule.nextMonth, rule.dayOfMonth)
}

data class RecurringListUiState(
    val items: List<RecurringItem> = emptyList(),
    val loading: Boolean = true,
) {
    /** Tổng chi cố định mỗi tháng từ các quy tắc đang bật. */
    val monthlyExpense: Long
        get() = items.filter { it.rule.active && it.rule.type == TransactionType.EXPENSE }.sumOf { it.rule.amount }

    val monthlyIncome: Long
        get() = items.filter { it.rule.active && it.rule.type == TransactionType.INCOME }.sumOf { it.rule.amount }
}

class RecurringListViewModel(private val repository: FinanceRepository) : ViewModel() {

    val state: StateFlow<RecurringListUiState> =
        combine(repository.recurringRules, repository.categories) { rules, categories ->
            val byId = categories.associateBy { it.id }
            RecurringListUiState(
                items = rules.mapNotNull { r -> byId[r.categoryId]?.let { RecurringItem(r, it) } },
                loading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringListUiState())

    fun setActive(rule: RecurringRule, active: Boolean) {
        viewModelScope.launch { repository.saveRecurring(rule.copy(active = active)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RecurringListViewModel(container.repository) }
        }
    }
}
