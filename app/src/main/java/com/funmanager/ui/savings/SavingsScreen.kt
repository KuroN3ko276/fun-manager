package com.funmanager.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funmanager.R
import com.funmanager.data.GoalWithSaved
import com.funmanager.data.SavingsGoal
import com.funmanager.domain.formatVnd
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.theme.FunManagerTheme
import java.time.LocalDate

@Composable
fun SavingsRoute(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: SavingsViewModel = viewModel(factory = SavingsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SavingsScreen(state, onAdd, onOpen)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(state: SavingsUiState, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_savings)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_goal))
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.total_saved), style = MaterialTheme.typography.labelLarge)
                        MoneyText(state.totalSaved, style = MaterialTheme.typography.headlineMedium)
                        if (state.totalTarget > 0) {
                            Text(
                                stringResource(R.string.of_total_target, formatVnd(state.totalTarget)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (!state.loading && state.goals.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.goals_empty),
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.goals, key = { it.goal.id }) { item ->
                GoalCard(item, onClick = { onOpen(item.goal.id) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SavingsScreenPreview() {
    FunManagerTheme {
        SavingsScreen(
            SavingsUiState(
                goals = listOf(
                    GoalWithSaved(
                        SavingsGoal(1, "Mua xe máy", 40_000_000, "transport", 0xFF42A5F5, LocalDate.of(2027, 6, 30)),
                        12_500_000,
                    ),
                    GoalWithSaved(SavingsGoal(2, "Quỹ khẩn cấp", 30_000_000, "savings", 0xFF66BB6A), 30_000_000),
                ),
                loading = false,
            ),
            onAdd = {},
            onOpen = {},
        )
    }
}
