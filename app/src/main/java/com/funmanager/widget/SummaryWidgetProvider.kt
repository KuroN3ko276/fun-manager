package com.funmanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.funmanager.FunManagerApp
import com.funmanager.MainActivity
import com.funmanager.R
import com.funmanager.data.WidgetData
import com.funmanager.domain.BudgetLevel
import com.funmanager.domain.WidgetCategoryLine
import com.funmanager.domain.formatVnd
import com.funmanager.ui.components.label
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * Widget màn hình chính: tổng chi tháng (tuỳ chọn) và số tiền còn lại của các danh mục đã chọn.
 * Không hiển thị thu nhập.
 */
class SummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                appWidgetIds.forEach { render(context, manager, it) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val store = WidgetConfigStore(context)
        appWidgetIds.forEach(store::delete)
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val ROWS = listOf(
            Row(R.id.cat_row_1, R.id.cat_dot_1, R.id.cat_name_1, R.id.cat_amount_1, R.id.cat_progress_1),
            Row(R.id.cat_row_2, R.id.cat_dot_2, R.id.cat_name_2, R.id.cat_amount_2, R.id.cat_progress_2),
            Row(R.id.cat_row_3, R.id.cat_dot_3, R.id.cat_name_3, R.id.cat_amount_3, R.id.cat_progress_3),
            Row(R.id.cat_row_4, R.id.cat_dot_4, R.id.cat_name_4, R.id.cat_amount_4, R.id.cat_progress_4),
        )

        /** Vẽ lại mọi widget đang đặt trên màn hình chính. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SummaryWidgetProvider::class.java))
            if (ids.isEmpty()) return
            scope.launch { ids.forEach { render(context, manager, it) } }
        }

        /** Vẽ lại một widget (sau khi lưu cấu hình). */
        fun update(context: Context, widgetId: Int) {
            scope.launch { render(context, AppWidgetManager.getInstance(context), widgetId) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val app = context.applicationContext as FunManagerApp
            val config = WidgetConfigStore(context).get(widgetId)
            val data = app.container.repository.widgetData(YearMonth.now(), config.categoryIds)
            manager.updateAppWidget(widgetId, buildViews(context, widgetId, config, data))
        }

        private fun buildViews(context: Context, widgetId: Int, config: WidgetConfig, data: WidgetData): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_summary)
            val s = data.snapshot
            views.setTextViewText(R.id.widget_month, s.month.label())

            views.setViewVisibility(R.id.widget_total, if (config.showTotal) View.VISIBLE else View.GONE)
            if (config.showTotal) {
                views.setTextViewText(R.id.widget_expense, formatVnd(s.expense))
                val budget = s.budget
                if (budget == null) {
                    views.setViewVisibility(R.id.widget_progress, View.GONE)
                    views.setViewVisibility(R.id.widget_detail, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_detail, View.VISIBLE)
                    views.setProgressBar(R.id.widget_progress, 100, s.budgetPercent, false)
                    tintProgress(context, views, R.id.widget_progress, budget.level)
                    val text = if (budget.level == BudgetLevel.EXCEEDED) {
                        context.getString(R.string.widget_over_budget, formatVnd(-budget.remaining), formatVnd(budget.limit))
                    } else {
                        context.getString(R.string.widget_left_budget, formatVnd(budget.remaining), formatVnd(budget.limit))
                    }
                    views.setTextViewText(R.id.widget_detail, text)
                }
            }

            ROWS.forEachIndexed { i, row ->
                val line = data.lines.getOrNull(i)
                if (line == null) {
                    views.setViewVisibility(row.root, View.GONE)
                } else {
                    bindRow(context, views, row, line)
                }
            }
            val nothing = data.lines.isEmpty() && !config.showTotal
            views.setViewVisibility(R.id.widget_empty, if (data.lines.isEmpty()) View.VISIBLE else View.GONE)
            if (nothing) views.setViewVisibility(R.id.widget_total, View.GONE)

            views.setOnClickPendingIntent(R.id.widget_root, activityIntent(context, add = false))
            views.setOnClickPendingIntent(R.id.widget_add, activityIntent(context, add = true))
            views.setOnClickPendingIntent(R.id.widget_settings, configIntent(context, widgetId))
            return views
        }

        private fun bindRow(context: Context, views: RemoteViews, row: Row, line: WidgetCategoryLine) {
            views.setViewVisibility(row.root, View.VISIBLE)
            views.setInt(row.dot, "setColorFilter", line.category.color.toInt())
            views.setTextViewText(row.name, line.category.name)
            val budget = line.budget
            if (budget == null) {
                views.setTextViewText(row.amount, context.getString(R.string.widget_cat_spent, formatVnd(line.spent)))
                views.setTextColor(row.amount, context.getColor(R.color.widget_text_secondary))
                views.setViewVisibility(row.progress, View.GONE)
            } else {
                val text = if (budget.level == BudgetLevel.EXCEEDED) {
                    context.getString(R.string.widget_cat_over, formatVnd(-budget.remaining))
                } else {
                    context.getString(R.string.widget_cat_left, formatVnd(budget.remaining))
                }
                views.setTextViewText(row.amount, text)
                views.setTextColor(row.amount, context.getColor(levelColor(budget.level)))
                views.setViewVisibility(row.progress, View.VISIBLE)
                views.setProgressBar(row.progress, 100, (budget.ratio * 100).toInt().coerceIn(0, 100), false)
                tintProgress(context, views, row.progress, budget.level)
            }
        }

        private fun levelColor(level: BudgetLevel) = when (level) {
            BudgetLevel.OK -> R.color.widget_accent
            BudgetLevel.WARNING -> R.color.widget_warning
            BudgetLevel.EXCEEDED -> R.color.widget_expense
        }

        private fun tintProgress(context: Context, views: RemoteViews, id: Int, level: BudgetLevel) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(id, "setProgressTintList", ColorStateList.valueOf(context.getColor(levelColor(level))))
            }
        }

        private fun activityIntent(context: Context, add: Boolean): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_ADD_TRANSACTION, add)
            return PendingIntent.getActivity(
                context,
                if (add) 1 else 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun configIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(WidgetConfigActivity.EXTRA_RECONFIGURE, true)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            return PendingIntent.getActivity(
                context,
                CONFIG_REQUEST_BASE + widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private const val CONFIG_REQUEST_BASE = 1000

        private data class Row(val root: Int, val dot: Int, val name: Int, val amount: Int, val progress: Int)
    }
}
