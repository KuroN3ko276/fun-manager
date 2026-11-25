package com.funmanager.ui.category

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.Category
import com.funmanager.data.DefaultCategories
import com.funmanager.data.TransactionType
import com.funmanager.domain.CategoryNameError
import com.funmanager.ui.components.CATEGORY_COLORS
import com.funmanager.ui.components.CATEGORY_ICONS
import com.funmanager.ui.components.CategoryIcon
import com.funmanager.ui.components.IconBubble
import com.funmanager.ui.components.iconFor
import com.funmanager.ui.theme.FunManagerTheme

@Composable
fun CategoriesRoute(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoriesScreen(
        state = state,
        onBack = onBack,
        onTypeChange = viewModel::onTypeChange,
        onAdd = viewModel::startCreate,
        onEdit = viewModel::startEdit,
    )
    state.editor?.let { editor ->
        CategoryEditorDialog(
            editor = editor,
            onChange = viewModel::onEditorChange,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveEditor,
            onDelete = viewModel::requestDelete,
        )
    }
    state.deleteRequest?.let { request ->
        DeleteCategoryDialog(
            request = request,
            onReplacementChange = viewModel::onReplacementChange,
            onDismiss = viewModel::dismissDelete,
            onConfirm = viewModel::confirmDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onBack: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Category) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, modifier = Modifier.navigationBarsPadding()) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_category))
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            item {
                TypeSelector(state.type, onTypeChange, Modifier.padding(vertical = 8.dp))
            }
            items(state.categories, key = { it.id }) { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(category) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIcon(category)
                    Spacer(Modifier.width(16.dp))
                    Text(category.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeSelector(type: TransactionType, onChange: (TransactionType) -> Unit, modifier: Modifier = Modifier) {
    val types = listOf(TransactionType.EXPENSE to R.string.expense, TransactionType.INCOME to R.string.income)
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        types.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = type == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, types.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    editor: CategoryEditor,
    onChange: ((CategoryEditor) -> CategoryEditor) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (editor.original == null) R.string.add_category else R.string.edit_category))
        },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(
                        iconFor(editor.icon),
                        Color(editor.color),
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = { name -> onChange { it.copy(name = name) } },
                        label = { Text(stringResource(R.string.category_name)) },
                        isError = editor.error != null,
                        supportingText = editor.error?.let { error ->
                            {
                                Text(
                                    stringResource(
                                        when (error) {
                                            CategoryNameError.EMPTY -> R.string.category_name_empty
                                            CategoryNameError.DUPLICATE -> R.string.category_name_duplicate
                                        },
                                    ),
                                )
                            }
                        },
                        singleLine = true,
                    )
                }
                Text(stringResource(R.string.icon), style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CATEGORY_ICONS.distinctBy { it.second }.forEach { (key, icon) ->
                        val selected = key == editor.icon
                        IconBubble(
                            icon = icon,
                            color = if (selected) Color(editor.color) else MaterialTheme.colorScheme.outline,
                            size = 40.dp,
                            modifier = Modifier
                                .clickable { onChange { it.copy(icon = key) } }
                                .then(
                                    if (selected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }
                }
                Text(stringResource(R.string.color), style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CATEGORY_COLORS.forEach { color ->
                        val selected = color == editor.color
                        IconBubble(
                            icon = Icons.Filled.Check,
                            color = Color(color),
                            size = 36.dp,
                            modifier = Modifier.clickable { onChange { it.copy(color = color) } },
                            iconTint = if (selected) Color.White else Color.Transparent,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                if (editor.original != null && editor.canDelete) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun DeleteCategoryDialog(
    request: DeleteRequest,
    onReplacementChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_category_title, request.category.name)) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                if (request.usage == 0) {
                    Text(stringResource(R.string.delete_category_unused))
                } else {
                    Text(stringResource(R.string.delete_category_move, request.usage))
                    request.replacements.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = request.replacementId == c.id,
                                    onClick = { onReplacementChange(c.id) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = request.replacementId == c.id, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            CategoryIcon(c, size = 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(c.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenPreview() {
    FunManagerTheme {
        CategoriesScreen(
            state = CategoriesUiState(all = DefaultCategories.all.mapIndexed { i, c -> c.copy(id = i + 1L) }),
            onBack = {},
            onTypeChange = {},
            onAdd = {},
            onEdit = {},
        )
    }
}
