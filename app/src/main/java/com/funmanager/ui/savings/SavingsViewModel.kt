package com.funmanager.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.FinanceRepository
import com.funmanager.data.GoalWithSaved
import com.funmanager.ui.container
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SavingsUiState(
    val goals: List<GoalWithSaved> = emptyList(),
    val loading: Boolean = true,
) {
    val totalSaved: Long get() = goals.sumOf { it.saved }
    val totalTarget: Long get() = goals.sumOf { it.goal.target }
}

class SavingsViewModel(repository: FinanceRepository) : ViewModel() {
    val state: StateFlow<SavingsUiState> = repository.savingsGoals
        .map { SavingsUiState(goals = it, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer { SavingsViewModel(container.repository) }
        }
    }
}
