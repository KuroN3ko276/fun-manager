package com.funmanager.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.Category
import com.funmanager.data.FinanceRepository
import com.funmanager.data.TransactionType
import com.funmanager.domain.DatePreset
import com.funmanager.domain.TransactionFilter
import com.funmanager.domain.filterTransactions
import com.funmanager.domain.parseVnd
import com.funmanager.ui.components.TransactionItem
import com.funmanager.ui.components.formatAmountInput
import com.funmanager.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Bộ lọc đang soạn trên giao diện (số tiền dạng chuỗi để giữ dấu chấm khi gõ). */
data class FilterInput(
    val filter: TransactionFilter = TransactionFilter(),
    val minText: String = "",
    val maxText: String = "",
)

data class SearchUiState(
    val input: FilterInput = FilterInput(),
    val categories: List<Category> = emptyList(),
    val results: Map<LocalDate, List<TransactionItem>> = emptyMap(),
    val count: Int = 0,
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val showFilters: Boolean = false,
)

class SearchViewModel(repository: FinanceRepository) : ViewModel() {

    private val input = MutableStateFlow(FilterInput())
    private val showFilters = MutableStateFlow(false)

    val state: StateFlow<SearchUiState> = combine(
        input,
        showFilters,
        repository.allTransactions,
        repository.categories,
    ) { input, show, transactions, categories ->
        val byId = categories.associateBy { it.id }
        val matched = filterTransactions(transactions, byId, input.filter, LocalDate.now())
        SearchUiState(
            input = input,
            categories = categories,
            results = matched
                .mapNotNull { t -> byId[t.categoryId]?.let { TransactionItem(t, it) } }
                .groupBy { it.transaction.date },
            count = matched.size,
            totalIncome = matched.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            totalExpense = matched.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            showFilters = show,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    private fun updateFilter(transform: (TransactionFilter) -> TransactionFilter) =
        input.update { it.copy(filter = transform(it.filter)) }

    fun onQueryChange(query: String) = updateFilter { it.copy(query = query) }

    fun toggleFilters() = showFilters.update { !it }

    fun onTypeChange(type: TransactionType?) = updateFilter { f ->
        // Bỏ các danh mục không còn khớp loại đã chọn.
        val keep = if (type == null) f.categoryIds else state.value.categories.filter { it.type == type }.map { it.id }.toSet()
        f.copy(type = type, categoryIds = f.categoryIds intersect keep)
    }

    fun onToggleCategory(id: Long) = updateFilter { f ->
        f.copy(categoryIds = if (id in f.categoryIds) f.categoryIds - id else f.categoryIds + id)
    }

    fun onDatePreset(preset: DatePreset) = updateFilter { it.copy(datePreset = preset) }

    fun onCustomRange(from: LocalDate?, to: LocalDate?) =
        updateFilter { it.copy(datePreset = DatePreset.CUSTOM, customFrom = from, customTo = to) }

    fun onMinChange(text: String) = input.update {
        val formatted = formatAmountInput(text)
        it.copy(minText = formatted, filter = it.filter.copy(minAmount = parseVnd(formatted)))
    }

    fun onMaxChange(text: String) = input.update {
        val formatted = formatAmountInput(text)
        it.copy(maxText = formatted, filter = it.filter.copy(maxAmount = parseVnd(formatted)))
    }

    fun clearFilters() = input.update { FilterInput(filter = TransactionFilter(query = it.filter.query)) }

    companion object {
        val Factory = viewModelFactory {
            initializer { SearchViewModel(container.repository) }
        }
    }
}
