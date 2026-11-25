package com.funmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.Category
import com.funmanager.data.FinanceRepository
import com.funmanager.data.Transaction
import com.funmanager.domain.MonthlySummary
import com.funmanager.domain.summarize
import com.funmanager.ui.container
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class TransactionItem(val transaction: Transaction, val category: Category)

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val summary: MonthlySummary = MonthlySummary.EMPTY,
    val days: Map<LocalDate, List<TransactionItem>> = emptyMap(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    repository: FinanceRepository,
    private val selectedMonth: MutableStateFlow<YearMonth>,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        selectedMonth,
        selectedMonth.flatMapLatest { repository.transactionsIn(it) },
        repository.categories,
    ) { month, transactions, categories ->
        val byId = categories.associateBy { it.id }
        HomeUiState(
            month = month,
            summary = summarize(transactions, categories),
            days = transactions
                .mapNotNull { t -> byId[t.categoryId]?.let { TransactionItem(t, it) } }
                .groupBy { it.transaction.date },
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onMonthChange(month: YearMonth) {
        selectedMonth.value = month
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(container.repository, container.selectedMonth) }
        }
    }
}
