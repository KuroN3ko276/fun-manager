package com.funmanager.ui.savings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.ui.components.AmountField
import com.funmanager.ui.components.CATEGORY_COLORS
import com.funmanager.ui.components.CATEGORY_ICONS
import com.funmanager.ui.components.IconBubble
import com.funmanager.ui.components.iconFor
import com.funmanager.ui.components.shortDate
import com.funmanager.ui.theme.FunManagerTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun EditGoalRoute(
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: EditGoalViewModel = viewModel(factory = EditGoalViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.result) {
        when (state.result) {
            EditGoalResult.SAVED -> onSaved()
            EditGoalResult.DELETED -> onDeleted()
            null -> Unit
        }
    }
    EditGoalScreen(
        state = state,
        onBack = onSaved,
        onNameChange = viewModel::onNameChange,
        onTargetChange = viewModel::onTargetChange,
        onDeadlineChange = viewModel::onDeadlineChange,
        onIconChange = viewModel::onIconChange,
        onColorChange = viewModel::onColorChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditGoalScreen(
    state: EditGoalUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onDeadlineChange: (LocalDate?) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.add_goal else R.string.edit_goal)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(iconFor(state.icon), Color(state.color), size = 48.dp)
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.goal_name)) },
                    isError = state.nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AmountField(state.targetText, onTargetChange, state.targetError, label = stringResource(R.string.goal_target))

            Text(stringResource(R.string.goal_deadline), style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  " + (state.deadline?.shortDate() ?: stringResource(R.string.no_deadline)))
                }
                if (state.deadline != null) {
                    IconButton(onClick = { onDeadlineChange(null) }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.clear_deadline))
                    }
                }
            }

            Text(stringResource(R.string.icon), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORY_ICONS.distinctBy { it.second }.forEach { (key, icon) ->
                    val selected = key == state.icon
                    IconBubble(
                        icon = icon,
                        color = if (selected) Color(state.color) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .clickable { onIconChange(key) }
                            .then(
                                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier,
                            ),
                    )
                }
            }

            Text(stringResource(R.string.color), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORY_COLORS.forEach { color ->
                    IconBubble(
                        icon = Icons.Filled.Check,
                        color = Color(color),
                        size = 36.dp,
                        iconTint = if (color == state.color) Color.White else Color.Transparent,
                        modifier = Modifier.clickable { onColorChange(color) },
                    )
                }
            }

            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (state.deadline ?: LocalDate.now().plusMonths(6))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDeadlineChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(pickerState) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_goal_title)) },
            text = { Text(stringResource(R.string.delete_goal_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditGoalPreview() {
    FunManagerTheme {
        EditGoalScreen(
            state = EditGoalUiState(name = "Mua xe máy", targetText = "40.000.000", deadline = LocalDate.of(2027, 6, 30)),
            onBack = {},
            onNameChange = {},
            onTargetChange = {},
            onDeadlineChange = {},
            onIconChange = {},
            onColorChange = {},
            onSave = {},
            onDelete = {},
        )
    }
}
