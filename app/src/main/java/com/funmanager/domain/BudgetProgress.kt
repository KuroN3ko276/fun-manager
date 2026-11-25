package com.funmanager.domain

import com.funmanager.data.Budget
import com.funmanager.data.Category
import com.funmanager.data.TransactionType

enum class BudgetLevel { OK, WARNING, EXCEEDED }

data class BudgetProgress(val spent: Long, val limit: Long) {
    /** Tỷ lệ đã chi trên hạn mức, có thể > 1 khi vượt. */
    val ratio: Float get() = if (limit <= 0) 0f else spent.toFloat() / limit

    val remaining: Long get() = limit - spent

    val level: BudgetLevel
        get() = when {
            limit <= 0 -> BudgetLevel.OK
            spent > limit -> BudgetLevel.EXCEEDED
            spent * 100 >= limit * WARNING_PERCENT -> BudgetLevel.WARNING
            else -> BudgetLevel.OK
        }

    companion object {
        const val WARNING_PERCENT = 80
    }
}

data class CategoryBudget(
    val category: Category,
    val spent: Long,
    /** null nếu danh mục chưa đặt ngân sách. */
    val progress: BudgetProgress?,
)

data class BudgetOverview(
    val totalSpent: Long,
    val total: BudgetProgress?,
    val categories: List<CategoryBudget>,
) {
    /** Các mục (tổng hoặc danh mục) đang ở mức cảnh báo hoặc vượt. */
    val alerts: List<CategoryBudget>
        get() = categories.filter { (it.progress?.level ?: BudgetLevel.OK) != BudgetLevel.OK }

    val hasAlert: Boolean
        get() = (total?.level ?: BudgetLevel.OK) != BudgetLevel.OK || alerts.isNotEmpty()

    companion object {
        val EMPTY = BudgetOverview(0, null, emptyList())
    }
}

/** Gộp ngân sách với số đã chi; danh mục có ngân sách xếp trước, sau đó theo số tiền đã chi. */
fun budgetOverview(summary: MonthlySummary, budgets: List<Budget>, categories: List<Category>): BudgetOverview {
    val limits = budgets.associate { it.categoryId to it.limit }
    val spentById = summary.expenseByCategory.associate { it.category.id to it.amount }
    val rows = categories
        .filter { it.type == TransactionType.EXPENSE }
        .map { c ->
            val spent = spentById[c.id] ?: 0L
            CategoryBudget(c, spent, limits[c.id]?.let { BudgetProgress(spent, it) })
        }
        .sortedWith(compareBy<CategoryBudget> { it.progress == null }.thenByDescending { it.spent })
    return BudgetOverview(
        totalSpent = summary.expense,
        total = limits[Budget.TOTAL_CATEGORY_ID]?.let { BudgetProgress(summary.expense, it) },
        categories = rows,
    )
}
