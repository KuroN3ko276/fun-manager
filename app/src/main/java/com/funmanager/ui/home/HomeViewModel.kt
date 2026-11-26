package com.funmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.Budget
import com.funmanager.data.Category
import com.funmanager.data.FinanceRepository
import com.funmanager.data.RecurringRule
import com.funmanager.data.Transaction
import com.funmanager.data.Wallet
import com.funmanager.domain.BudgetOverview
import com.funmanager.domain.DailyAllowance
import com.funmanager.domain.MonthlySummary
import com.funmanager.domain.budgetOverview
import com.funmanager.domain.dailyAllowance
import com.funmanager.domain.summarize
import com.funmanager.ui.components.TransactionItem
import com.funmanager.ui.components.walletNames
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

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val summary: MonthlySummary = MonthlySummary.EMPTY,
    val days: Map<LocalDate, List<TransactionItem>> = emptyMap(),
    val budget: BudgetOverview = BudgetOverview.EMPTY,
    /** Chỉ có khi đang xem tháng hiện tại và đã đặt ngân sách. */
    val allowance: DailyAllowance? = null,
    /** Tên ví theo id; rỗng khi chỉ có một ví (không cần hiển thị). */
    val walletNames: Map<Long, String> = emptyMap(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    repository: FinanceRepository,
    private val selectedMonth: MutableStateFlow<YearMonth>,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = selectedMonth.flatMapLatest { month ->
        combine(
            repository.transactionsIn(month),
            repository.budgetsIn(month),
            repository.categories,
            repository.wallets,
            repository.recurringRules,
        ) { transactions, budgets, categories, wallets, rules ->
            buildState(month, transactions, budgets, categories, wallets, rules)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildState(
        month: YearMonth,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        categories: List<Category>,
        wallets: List<Wallet>,
        rules: List<RecurringRule>,
    ): HomeUiState {
        val today = LocalDate.now()
        val byId = categories.associateBy { it.id }
        val summary = summarize(transactions, categories)
        return HomeUiState(
            month = month,
            summary = summary,
            budget = budgetOverview(summary, budgets, categories),
            allowance = if (month == YearMonth.from(today)) dailyAllowance(today, budgets, transactions, rules) else null,
            days = transactions
                .mapNotNull { t -> byId[t.categoryId]?.let { TransactionItem(t, it) } }
                .groupBy { it.transaction.date },
            walletNames = walletNames(wallets),
            loading = false,
        )
    }

    fun onMonthChange(month: YearMonth) {
        selectedMonth.value = month
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(container.repository, container.selectedMonth) }
        }
    }
}
