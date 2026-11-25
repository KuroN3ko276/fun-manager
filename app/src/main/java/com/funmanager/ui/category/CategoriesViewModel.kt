package com.funmanager.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.data.Category
import com.funmanager.data.FinanceRepository
import com.funmanager.data.TransactionType
import com.funmanager.domain.CategoryNameError
import com.funmanager.domain.canDeleteCategory
import com.funmanager.domain.validateCategoryName
import com.funmanager.ui.components.CATEGORY_COLORS
import com.funmanager.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryEditor(
    /** null khi tạo mới. */
    val original: Category?,
    val type: TransactionType,
    val name: String,
    val icon: String,
    val color: Long,
    val error: CategoryNameError? = null,
    val canDelete: Boolean = false,
)

data class DeleteRequest(
    val category: Category,
    val usage: Int,
    val replacements: List<Category>,
    val replacementId: Long?,
)

data class CategoriesUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val all: List<Category> = emptyList(),
    val editor: CategoryEditor? = null,
    val deleteRequest: DeleteRequest? = null,
) {
    val categories: List<Category> get() = all.filter { it.type == type }
}

class CategoriesViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val ui = MutableStateFlow(CategoriesUiState())

    val state: StateFlow<CategoriesUiState> = combine(ui, repository.categories) { s, all -> s.copy(all = all) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onTypeChange(type: TransactionType) = ui.update { it.copy(type = type) }

    fun startCreate() = ui.update {
        it.copy(
            editor = CategoryEditor(
                original = null,
                type = it.type,
                name = "",
                icon = "other",
                color = CATEGORY_COLORS.first(),
            ),
        )
    }

    fun startEdit(category: Category) = ui.update {
        it.copy(
            editor = CategoryEditor(
                original = category,
                type = category.type,
                name = category.name,
                icon = category.icon,
                color = category.color,
                canDelete = canDeleteCategory(category, state.value.all),
            ),
        )
    }

    fun onEditorChange(transform: (CategoryEditor) -> CategoryEditor) =
        ui.update { s -> s.copy(editor = s.editor?.let(transform)?.copy(error = null)) }

    fun dismissEditor() = ui.update { it.copy(editor = null) }

    fun saveEditor() {
        val editor = ui.value.editor ?: return
        val error = validateCategoryName(editor.name, editor.original, state.value.all, editor.type)
        if (error != null) {
            ui.update { it.copy(editor = editor.copy(error = error)) }
            return
        }
        viewModelScope.launch {
            repository.saveCategory(
                Category(
                    id = editor.original?.id ?: 0,
                    name = editor.name.trim(),
                    icon = editor.icon,
                    color = editor.color,
                    type = editor.type,
                ),
            )
            ui.update { it.copy(editor = null) }
        }
    }

    fun requestDelete() {
        val category = ui.value.editor?.original ?: return
        viewModelScope.launch {
            val usage = repository.transactionCount(category.id)
            val replacements = state.value.all.filter { it.type == category.type && it.id != category.id }
            ui.update {
                it.copy(
                    editor = null,
                    deleteRequest = DeleteRequest(category, usage, replacements, replacements.firstOrNull()?.id),
                )
            }
        }
    }

    fun onReplacementChange(id: Long) = ui.update { s ->
        s.copy(deleteRequest = s.deleteRequest?.copy(replacementId = id))
    }

    fun dismissDelete() = ui.update { it.copy(deleteRequest = null) }

    fun confirmDelete() {
        val request = ui.value.deleteRequest ?: return
        viewModelScope.launch {
            repository.deleteCategory(request.category.id, request.replacementId.takeIf { request.usage > 0 })
            ui.update { it.copy(deleteRequest = null) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CategoriesViewModel(container.repository) }
        }
    }
}
