package com.funmanager.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.funmanager.FunManagerApp
import com.funmanager.R
import com.funmanager.data.Category
import com.funmanager.data.TransactionType
import com.funmanager.domain.WIDGET_MAX_CATEGORIES
import com.funmanager.ui.components.AppBackground
import com.funmanager.ui.components.CategoryIcon
import com.funmanager.ui.components.GlassCard
import com.funmanager.ui.theme.FunManagerTheme
import kotlinx.coroutines.launch
import java.time.YearMonth

/** Chọn danh mục và tuỳ chọn hiển thị cho một widget; mở khi thêm widget hoặc bấm ⚙ trên widget. */
class WidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Huỷ ngang khi thêm mới → launcher không đặt widget.
        setResult(RESULT_CANCELED, resultIntent())
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        val store = WidgetConfigStore(this)
        val initial = store.get(widgetId)
        val container = (application as FunManagerApp).container
        val repository = container.repository

        setContent {
            FunManagerTheme(container.accent.value) {
                var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
                var showTotal by remember { mutableStateOf(initial.showTotal) }
                val selected = remember { mutableStateListOf<Long>() }
                LaunchedEffect(Unit) {
                    val all = repository.allCategories().filter { it.type == TransactionType.EXPENSE }
                    categories = all
                    val start = initial.categoryIds
                        ?: repository.widgetData(YearMonth.now(), null).lines.map { it.category.id }
                    selected.addAll(start.filter { id -> all.any { it.id == id } })
                }
                WidgetConfigScreen(
                    categories = categories,
                    selected = selected,
                    showTotal = showTotal,
                    onShowTotal = { showTotal = it },
                    onToggle = { id ->
                        if (id in selected) {
                            selected.remove(id)
                        } else if (selected.size < WIDGET_MAX_CATEGORIES) {
                            selected.add(id)
                        }
                    },
                    onCancel = ::finish,
                    onSave = {
                        store.save(widgetId, WidgetConfig(showTotal = showTotal, categoryIds = selected.toList()))
                        lifecycleScope.launch {
                            SummaryWidgetProvider.update(this@WidgetConfigActivity, widgetId)
                            setResult(RESULT_OK, resultIntent())
                            finish()
                        }
                    },
                )
            }
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

    companion object {
        const val EXTRA_RECONFIGURE = "com.funmanager.extra.RECONFIGURE"
    }
}

@Composable
private fun WidgetConfigScreen(
    categories: List<Category>,
    selected: List<Long>,
    showTotal: Boolean,
    onShowTotal: (Boolean) -> Unit,
    onToggle: (Long) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    AppBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Text(
                stringResource(R.string.widget_config_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onShowTotal(!showTotal) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.widget_show_total), modifier = Modifier.weight(1f))
                            Switch(checked = showTotal, onCheckedChange = onShowTotal)
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.widget_pick_categories, WIDGET_MAX_CATEGORIES),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        stringResource(R.string.widget_pick_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(categories, key = { it.id }) { category ->
                    val checked = category.id in selected
                    val enabled = checked || selected.size < WIDGET_MAX_CATEGORIES
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onToggle(category.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggle(category.id) }, enabled = enabled)
                        CategoryIcon(category, size = 32.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(category.name, modifier = Modifier.weight(1f))
                        if (checked) {
                            Text(
                                "#${selected.indexOf(category.id) + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Button(onClick = onSave) { Text(stringResource(R.string.save)) }
            }
        }
    }
}
