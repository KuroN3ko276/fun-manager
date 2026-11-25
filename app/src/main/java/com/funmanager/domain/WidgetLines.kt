package com.funmanager.domain

import com.funmanager.data.Budget
import com.funmanager.data.Category

const val WIDGET_MAX_CATEGORIES = 4

/** Một dòng danh mục trên widget. */
data class WidgetCategoryLine(
    val category: Category,
    val spent: Long,
    /** null nếu danh mục chưa có ngân sách trong tháng. */
    val budget: BudgetProgress?,
)

/**
 * Các dòng danh mục cho widget theo thứ tự đã chọn; bỏ danh mục đã bị xoá.
 * Nếu chưa chọn gì: lấy các danh mục có ngân sách trong tháng (tối đa [WIDGET_MAX_CATEGORIES]).
 */
fun widgetCategoryLines(
    selectedIds: List<Long>?,
    categories: List<Category>,
    spentByCategory: Map<Long, Long>,
    budgets: List<Budget>,
): List<WidgetCategoryLine> {
    val byId = categories.associateBy { it.id }
    val limits = budgets.filter { it.categoryId != Budget.TOTAL_CATEGORY_ID }.associate { it.categoryId to it.limit }
    val ids = selectedIds ?: limits.keys.filter { byId[it] != null }.sortedByDescending { spentByCategory[it] ?: 0 }
    return ids
        .mapNotNull { byId[it] }
        .take(WIDGET_MAX_CATEGORIES)
        .map { c ->
            val spent = spentByCategory[c.id] ?: 0
            WidgetCategoryLine(c, spent, limits[c.id]?.let { BudgetProgress(spent, it) })
        }
}
