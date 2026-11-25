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
import com.funmanager.domain.BudgetLevel
import com.funmanager.domain.MonthSnapshot
import com.funmanager.domain.formatVnd
import com.funmanager.ui.components.label
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.YearMonth

/** Widget màn hình chính: số đã chi tháng này, ngân sách còn lại / số dư, nút thêm nhanh. */
class SummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                render(context, manager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Vẽ lại mọi widget đang đặt trên màn hình chính. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SummaryWidgetProvider::class.java))
            if (ids.isEmpty()) return
            scope.launch { render(context, manager, ids) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val app = context.applicationContext as FunManagerApp
            val snapshot = app.container.repository.monthSnapshot(YearMonth.now())
            val views = buildViews(context, snapshot)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context, s: MonthSnapshot): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_summary)
            views.setTextViewText(R.id.widget_month, s.month.label())
            views.setTextViewText(R.id.widget_expense, formatVnd(s.expense))

            val budget = s.budget
            if (budget == null) {
                views.setViewVisibility(R.id.widget_progress, View.GONE)
                views.setTextViewText(
                    R.id.widget_detail,
                    context.getString(R.string.widget_no_budget, formatVnd(s.income), formatVnd(s.balance)),
                )
            } else {
                views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
                views.setProgressBar(R.id.widget_progress, 100, s.budgetPercent, false)
                val colorRes = when (budget.level) {
                    BudgetLevel.OK -> R.color.widget_accent
                    BudgetLevel.WARNING -> R.color.widget_warning
                    BudgetLevel.EXCEEDED -> R.color.widget_expense
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    views.setColorStateList(
                        R.id.widget_progress,
                        "setProgressTintList",
                        ColorStateList.valueOf(context.getColor(colorRes)),
                    )
                }
                val text = if (budget.level == BudgetLevel.EXCEEDED) {
                    context.getString(R.string.widget_over_budget, formatVnd(-budget.remaining), formatVnd(budget.limit))
                } else {
                    context.getString(R.string.widget_left_budget, formatVnd(budget.remaining), formatVnd(budget.limit))
                }
                views.setTextViewText(R.id.widget_detail, text)
            }

            views.setOnClickPendingIntent(R.id.widget_root, activityIntent(context, add = false))
            views.setOnClickPendingIntent(R.id.widget_add, activityIntent(context, add = true))
            return views
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
    }
}
