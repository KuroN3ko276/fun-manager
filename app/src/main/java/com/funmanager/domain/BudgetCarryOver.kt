package com.funmanager.domain

import com.funmanager.data.Budget
import java.time.YearMonth

/**
 * Hạn mức đang áp dụng cho [month]: với mỗi danh mục (và ngân sách tổng), lấy lần đặt gần nhất
 * ở [month] hoặc trước đó. Hạn mức ≤ 0 là dấu "đã bỏ ngân sách từ tháng đó" nên bị loại.
 * Mỗi phần tử giữ nguyên `month` là tháng bắt đầu áp dụng.
 */
fun effectiveBudgets(all: List<Budget>, month: YearMonth): List<Budget> =
    all.asSequence()
        .filter { it.month <= month }
        .groupBy { it.categoryId }
        .values
        .map { entries -> entries.maxBy { it.month } }
        .filter { it.limit > 0 }
        .sortedBy { it.categoryId }

/** Bản ghi cần lưu khi đặt hạn mức [limit] (null/≤0 = bỏ) cho [categoryId] từ [month] trở đi. */
sealed interface BudgetChange {
    data class Upsert(val budget: Budget) : BudgetChange
    data class Delete(val month: YearMonth, val categoryId: Long) : BudgetChange
}

fun budgetChange(all: List<Budget>, month: YearMonth, categoryId: Long, limit: Long?): BudgetChange {
    if (limit != null && limit > 0) return BudgetChange.Upsert(Budget(month, categoryId, limit))
    // Bỏ ngân sách: nếu tháng trước còn hạn mức đang chảy xuống thì cần một bản ghi "0" để chặn lại.
    val inheritsFromEarlier = effectiveBudgets(all.filter { it.month < month }, month.minusMonths(1))
        .any { it.categoryId == categoryId }
    return if (inheritsFromEarlier) {
        BudgetChange.Upsert(Budget(month, categoryId, 0))
    } else {
        BudgetChange.Delete(month, categoryId)
    }
}
