package com.funmanager.widget

import android.content.Context

/** Cấu hình riêng của từng widget (theo appWidgetId). */
data class WidgetConfig(
    /** Hiện tổng chi tháng và ngân sách tổng. */
    val showTotal: Boolean = true,
    /** Danh mục đã chọn theo thứ tự; null = chưa cấu hình (dùng mặc định). */
    val categoryIds: List<Long>? = null,
)

class WidgetConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("widget_config", Context.MODE_PRIVATE)

    fun get(widgetId: Int): WidgetConfig {
        if (!prefs.contains(keyTotal(widgetId))) return WidgetConfig()
        return WidgetConfig(
            showTotal = prefs.getBoolean(keyTotal(widgetId), true),
            categoryIds = prefs.getString(keyCategories(widgetId), "")
                .orEmpty()
                .split(',')
                .mapNotNull { it.toLongOrNull() },
        )
    }

    fun save(widgetId: Int, config: WidgetConfig) {
        prefs.edit()
            .putBoolean(keyTotal(widgetId), config.showTotal)
            .putString(keyCategories(widgetId), config.categoryIds.orEmpty().joinToString(","))
            .apply()
    }

    fun delete(widgetId: Int) {
        prefs.edit().remove(keyTotal(widgetId)).remove(keyCategories(widgetId)).apply()
    }

    private fun keyTotal(id: Int) = "w${id}_total"
    private fun keyCategories(id: Int) = "w${id}_categories"
}
