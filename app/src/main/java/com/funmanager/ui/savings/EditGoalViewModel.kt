package com.funmanager.ui.savings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.FinanceRepository
import com.funmanager.data.SavingsGoal
import com.funmanager.domain.groupDigits
import com.funmanager.domain.parseVnd
import com.funmanager.ui.components.CATEGORY_COLORS
import com.funmanager.ui.components.formatAmountInput
import com.funmanager.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class EditGoalResult { SAVED, DELETED }

data class EditGoalUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val targetText: String = "",
    val deadline: LocalDate? = null,
    val icon: String = "savings",
    val color: Long = CATEGORY_COLORS[8],
    val showErrors: Boolean = false,
    val result: EditGoalResult? = null,
) {
    val target: Long? get() = parseVnd(targetText)?.takeIf { it > 0 }
    val nameError: Boolean get() = showErrors && name.isBlank()
    val targetError: Boolean get() = showErrors && target == null
}

class EditGoalViewModel(
    private val repository: FinanceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: Long = savedStateHandle.get<Long>(ARG_ID) ?: NEW_ID

    private val _state = MutableStateFlow(EditGoalUiState(isNew = id == NEW_ID))
    val state: StateFlow<EditGoalUiState> = _state.asStateFlow()

    init {
        if (id != NEW_ID) {
            viewModelScope.launch {
                val goal = repository.getSavingsGoal(id) ?: return@launch
                _state.update {
                    it.copy(
                        name = goal.name,
                        targetText = groupDigits(goal.target.toString()),
                        deadline = goal.deadline,
                        icon = goal.icon,
                        color = goal.color,
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }

    fun onTargetChange(text: String) = _state.update { it.copy(targetText = formatAmountInput(text)) }

    fun onDeadlineChange(date: LocalDate?) = _state.update { it.copy(deadline = date) }

    fun onIconChange(icon: String) = _state.update { it.copy(icon = icon) }

    fun onColorChange(color: Long) = _state.update { it.copy(color = color) }

    fun save() {
        val s = _state.value
        val target = s.target
        if (s.name.isBlank() || target == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            repository.saveSavingsGoal(
                SavingsGoal(
                    id = if (id == NEW_ID) 0 else id,
                    name = s.name.trim(),
                    target = target,
                    icon = s.icon,
                    color = s.color,
                    deadline = s.deadline,
                ),
            )
            _state.update { it.copy(result = EditGoalResult.SAVED) }
        }
    }

    fun delete() {
        if (id == NEW_ID) return
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
            _state.update { it.copy(result = EditGoalResult.DELETED) }
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val NEW_ID = -1L

        val Factory = viewModelFactory {
            initializer { EditGoalViewModel(container.repository, createSavedStateHandle()) }
        }
    }
}
