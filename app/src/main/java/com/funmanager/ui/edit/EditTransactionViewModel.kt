package com.funmanager.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.Category
import com.funmanager.data.FinanceRepository
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import com.funmanager.domain.groupDigits
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

data class EditUiState(
    val isNew: Boolean = true,
    val type: TransactionType = TransactionType.EXPENSE,
    /** Số tiền đã định dạng có dấu chấm, ví dụ `1.250.000`. */
    val amountText: String = "",
    val categoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val allCategories: List<Category> = emptyList(),
    val showErrors: Boolean = false,
    val done: Boolean = false,
) {
    val categories: List<Category> get() = allCategories.filter { it.type == type }
    val amount: Long? get() = parseVnd(amountText)?.takeIf { it > 0 }
    val amountError: Boolean get() = showErrors && amount == null
    val categoryError: Boolean get() = showErrors && categoryId == null
}

class EditTransactionViewModel(
    private val repository: FinanceRepository,
    savedStateHandle: SavedStateHandle,
    selectedMonth: YearMonth,
) : ViewModel() {

    private val id: Long = savedStateHandle.get<Long>(ARG_ID) ?: NEW_ID
    private var original: Transaction? = null

    private val _state = MutableStateFlow(
        EditUiState(isNew = id == NEW_ID, date = defaultDate(selectedMonth)),
    )
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = repository.categories.first()
            val existing = if (id == NEW_ID) null else repository.transaction(id)
            original = existing
            _state.update { s ->
                if (existing == null) {
                    s.copy(
                        allCategories = categories,
                        categoryId = categories.firstOrNull { it.type == s.type }?.id,
                    )
                } else {
                    s.copy(
                        allCategories = categories,
                        type = existing.type,
                        amountText = groupDigits(existing.amount.toString()),
                        categoryId = existing.categoryId,
                        date = existing.date,
                        note = existing.note,
                    )
                }
            }
        }
    }

    fun onTypeChange(type: TransactionType) = _state.update { s ->
        if (s.type == type) {
            s
        } else {
            s.copy(type = type, categoryId = s.allCategories.firstOrNull { it.type == type }?.id)
        }
    }

    fun onAmountChange(text: String) = _state.update { it.copy(amountText = formatAmountInput(text)) }

    fun onCategoryChange(id: Long) = _state.update { it.copy(categoryId = id) }

    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date) }

    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }

    fun save() {
        val s = _state.value
        val amount = s.amount
        val categoryId = s.categoryId
        if (amount == null || categoryId == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            repository.save(
                Transaction(
                    id = original?.id ?: 0,
                    amount = amount,
                    type = s.type,
                    categoryId = categoryId,
                    date = s.date,
                    note = s.note.trim(),
                ),
            )
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val existing = original ?: return
        viewModelScope.launch {
            repository.delete(existing)
            _state.update { it.copy(done = true) }
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val NEW_ID = -1L

        private fun defaultDate(month: YearMonth): LocalDate {
            val today = LocalDate.now()
            return if (YearMonth.from(today) == month) today else month.atDay(1)
        }

        val Factory = viewModelFactory {
            initializer {
                EditTransactionViewModel(
                    container.repository,
                    createSavedStateHandle(),
                    container.selectedMonth.value,
                )
            }
        }
    }
}
