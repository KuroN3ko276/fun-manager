package com.funmanager.ui.recurring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.Category
import com.funmanager.data.FinanceRepository
import com.funmanager.data.RecurringRule
import com.funmanager.data.TransactionType
import com.funmanager.data.Wallet
import com.funmanager.domain.groupDigits
import com.funmanager.domain.occurrenceDate
import com.funmanager.domain.resumeMonth
import com.funmanager.domain.parseVnd
import com.funmanager.ui.components.formatAmountInput
import com.funmanager.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class EditRecurringUiState(
    val isNew: Boolean = true,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val categoryId: Long? = null,
    val dayOfMonth: Int = LocalDate.now().dayOfMonth,
    val note: String = "",
    val active: Boolean = true,
    /** Tháng kế tiếp đã lưu của quy tắc (null khi tạo mới). */
    val savedNextMonth: YearMonth? = null,
    val allCategories: List<Category> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val walletId: Long = Wallet.DEFAULT_ID,
    val showErrors: Boolean = false,
    val done: Boolean = false,
) {
    val categories: List<Category> get() = allCategories.filter { it.type == type }
    val amount: Long? get() = parseVnd(amountText)?.takeIf { it > 0 }
    val amountError: Boolean get() = showErrors && amount == null
    val categoryError: Boolean get() = showErrors && categoryId == null

    /** Ngày sẽ ghi lần tới nếu lưu bây giờ. */
    fun nextDate(today: LocalDate): LocalDate =
        occurrenceDate(resumeMonth(savedNextMonth, dayOfMonth, today), dayOfMonth)
}

class EditRecurringViewModel(
    private val repository: FinanceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: Long = savedStateHandle.get<Long>(ARG_ID) ?: NEW_ID
    private var original: RecurringRule? = null

    private val _state = MutableStateFlow(EditRecurringUiState(isNew = id == NEW_ID))
    val state: StateFlow<EditRecurringUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = repository.categories.first()
            val wallets = repository.wallets.first()
            val existing = if (id == NEW_ID) null else repository.recurringRule(id)
            original = existing
            _state.update { s ->
                if (existing == null) {
                    s.copy(
                        allCategories = categories,
                        wallets = wallets,
                        walletId = wallets.firstOrNull()?.id ?: Wallet.DEFAULT_ID,
                        categoryId = categories.firstOrNull { it.type == s.type }?.id,
                    )
                } else {
                    s.copy(
                        allCategories = categories,
                        wallets = wallets,
                        walletId = existing.walletId,
                        type = existing.type,
                        amountText = groupDigits(existing.amount.toString()),
                        categoryId = existing.categoryId,
                        dayOfMonth = existing.dayOfMonth,
                        note = existing.note,
                        active = existing.active,
                        savedNextMonth = existing.nextMonth,
                    )
                }
            }
        }
    }

    fun onTypeChange(type: TransactionType) = _state.update { s ->
        if (s.type == type) s else s.copy(type = type, categoryId = s.allCategories.firstOrNull { it.type == type }?.id)
    }

    fun onAmountChange(text: String) = _state.update { it.copy(amountText = formatAmountInput(text)) }

    fun onCategoryChange(id: Long) = _state.update { it.copy(categoryId = id) }

    fun onWalletChange(id: Long) = _state.update { it.copy(walletId = id) }

    fun onDayChange(day: Int) = _state.update { it.copy(dayOfMonth = day.coerceIn(1, 31)) }

    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }

    fun onActiveChange(active: Boolean) = _state.update { it.copy(active = active) }

    fun save() {
        val s = _state.value
        val amount = s.amount
        val categoryId = s.categoryId
        if (amount == null || categoryId == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            val today = LocalDate.now()
            repository.saveRecurring(
                RecurringRule(
                    id = original?.id ?: 0,
                    amount = amount,
                    type = s.type,
                    categoryId = categoryId,
                    dayOfMonth = s.dayOfMonth,
                    note = s.note.trim(),
                    walletId = s.walletId,
                    // Repository tính lại tháng kế tiếp, tránh tạo trùng hoặc tạo bù.
                    nextMonth = original?.nextMonth ?: YearMonth.from(today),
                    active = s.active,
                ),
                today,
            )
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val existing = original ?: return
        viewModelScope.launch {
            repository.deleteRecurring(existing.id)
            _state.update { it.copy(done = true) }
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val NEW_ID = -1L

        val Factory = viewModelFactory {
            initializer { EditRecurringViewModel(container.repository, createSavedStateHandle()) }
        }
    }
}
