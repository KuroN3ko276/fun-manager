package com.funmanager.domain

import java.time.YearMonth

/** Tóm tắt nhanh một tháng (dùng cho widget). */
data class MonthSnapshot(
    val month: YearMonth,
    val income: Long,
    val expense: Long,
    /** Ngân sách tổng của tháng, null nếu chưa đặt. */
    val totalBudget: Long?,
) {
    val balance: Long get() = income - expense

    val budget: BudgetProgress? get() = totalBudget?.let { BudgetProgress(expense, it) }

    /** Phần trăm ngân sách đã dùng, giới hạn 0..100 để vẽ thanh tiến độ. */
    val budgetPercent: Int get() = budget?.let { (it.ratio * 100).toInt().coerceIn(0, 100) } ?: 0
}
