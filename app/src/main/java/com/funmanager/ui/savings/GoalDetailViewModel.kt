package com.funmanager.ui.savings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.FinanceRepository
import com.funmanager.data.GoalWithSaved
import com.funmanager.data.SavingsDeposit
import com.funmanager.domain.canWithdraw
import com.funmanager.domain.parseVnd
import com.funmanager.ui.components.formatAmountInput
import com.funmanager.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AmountError { INVALID, EXCEEDS_SAVED }

data class MoveDialog(
    val withdraw: Boolean,
    val amountText: String = "",
    val note: String = "",
    val error: AmountError? = null,
)

data class GoalDetailUiState(
    val item: GoalWithSaved? = null,
    val deposits: List<SavingsDeposit> = emptyList(),
    val dialog: MoveDialog? = null,
    val pendingDelete: SavingsDeposit? = null,
    val loading: Boolean = true,
)

class GoalDetailViewModel(
    private val repository: FinanceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val goalId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_ID))

    private val ui = MutableStateFlow(GoalDetailUiState())

    val state: StateFlow<GoalDetailUiState> = combine(
        ui,
        repository.savingsGoal(goalId),
        repository.savingsDeposits(goalId),
    ) { s, item, deposits -> s.copy(item = item, deposits = deposits, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState())

    fun openDialog(withdraw: Boolean) = ui.update { it.copy(dialog = MoveDialog(withdraw)) }

    fun dismissDialog() = ui.update { it.copy(dialog = null) }

    fun onDialogAmount(text: String) = ui.update { s ->
        s.copy(dialog = s.dialog?.copy(amountText = formatAmountInput(text), error = null))
    }

    fun onDialogNote(note: String) = ui.update { s -> s.copy(dialog = s.dialog?.copy(note = note)) }

    fun confirmDialog() {
        val dialog = ui.value.dialog ?: return
        val amount = parseVnd(dialog.amountText)?.takeIf { it > 0 }
        val saved = state.value.item?.saved ?: 0
        val error = when {
            amount == null -> AmountError.INVALID
            dialog.withdraw && !canWithdraw(amount, saved) -> AmountError.EXCEEDS_SAVED
            else -> null
        }
        if (error != null || amount == null) {
            ui.update { it.copy(dialog = dialog.copy(error = error)) }
            return
        }
        viewModelScope.launch {
            repository.addSavingsDeposit(
                SavingsDeposit(
                    goalId = goalId,
                    amount = if (dialog.withdraw) -amount else amount,
                    date = LocalDate.now(),
                    note = dialog.note.trim(),
                ),
            )
            ui.update { it.copy(dialog = null) }
        }
    }

    fun requestDeleteDeposit(deposit: SavingsDeposit) = ui.update { it.copy(pendingDelete = deposit) }

    fun dismissDeleteDeposit() = ui.update { it.copy(pendingDelete = null) }

    fun confirmDeleteDeposit() {
        val deposit = ui.value.pendingDelete ?: return
        viewModelScope.launch {
            repository.deleteSavingsDeposit(deposit.id)
            ui.update { it.copy(pendingDelete = null) }
        }
    }

    companion object {
        const val ARG_ID = "id"

        val Factory = viewModelFactory {
            initializer { GoalDetailViewModel(container.repository, createSavedStateHandle()) }
        }
    }
}
