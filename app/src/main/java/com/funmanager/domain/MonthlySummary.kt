package com.funmanager.domain

import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType

data class CategoryTotal(
    val category: Category,
    val amount: Long,
    /** Tỷ lệ trên tổng của cùng loại (0f..1f). */
    val share: Float,
)

data class MonthlySummary(
    val income: Long,
    val expense: Long,
    val expenseByCategory: List<CategoryTotal>,
    val incomeByCategory: List<CategoryTotal>,
) {
    val balance: Long get() = income - expense

    companion object {
        val EMPTY = MonthlySummary(0, 0, emptyList(), emptyList())
    }
}

fun summarize(transactions: List<Transaction>, categories: List<Category>): MonthlySummary {
    val byId = categories.associateBy { it.id }
    fun totals(type: TransactionType): List<CategoryTotal> {
        val ofType = transactions.filter { it.type == type }
        val total = ofType.sumOf { it.amount }
        return ofType
            .groupBy { it.categoryId }
            .mapNotNull { (id, items) ->
                val category = byId[id] ?: return@mapNotNull null
                val amount = items.sumOf { it.amount }
                CategoryTotal(category, amount, if (total == 0L) 0f else amount.toFloat() / total)
            }
            .sortedByDescending { it.amount }
    }
    return MonthlySummary(
        income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        expenseByCategory = totals(TransactionType.EXPENSE),
        incomeByCategory = totals(TransactionType.INCOME),
    )
}
