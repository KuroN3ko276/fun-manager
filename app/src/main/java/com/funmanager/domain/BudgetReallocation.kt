package com.funmanager.domain

import com.funmanager.data.Budget
import com.funmanager.data.BudgetAdjustment
import java.time.YearMonth

/** Chênh lệch hạn mức theo danh mục do các lần chuyển: dương = nhận thêm, âm = chuyển đi. */
fun netAdjustments(adjustments: List<BudgetAdjustment>): Map<Long, Long> {
    val net = mutableMapOf<Long, Long>()
    adjustments.forEach { a ->
        net[a.fromCategoryId] = (net[a.fromCategoryId] ?: 0L) - a.amount
        net[a.toCategoryId] = (net[a.toCategoryId] ?: 0L) + a.amount
    }
    return net
}

/**
 * Áp các lần chuyển hạn mức của [month] lên hạn mức đang áp dụng [budgets] (kết quả của [effectiveBudgets]).
 * Chỉ ảnh hưởng tháng đó vì bảng `budgets` không đổi. Hạn mức không xuống dưới 0; danh mục chưa có
 * hạn mức mà nhận tiền thì có hạn mức bằng số nhận, bắt đầu từ [month].
 */
fun applyAdjustments(budgets: List<Budget>, adjustments: List<BudgetAdjustment>, month: YearMonth): List<Budget> {
    val net = netAdjustments(adjustments.filter { it.month == month })
    if (net.isEmpty()) return budgets
    val adjusted = budgets.map { b ->
        val delta = net[b.categoryId] ?: 0L
        if (delta == 0L || b.categoryId == Budget.TOTAL_CATEGORY_ID) b else b.copy(limit = (b.limit + delta).coerceAtLeast(0))
    }
    val existing = budgets.mapTo(HashSet()) { it.categoryId }
    val added = net
        .filter { (id, delta) -> id !in existing && id != Budget.TOTAL_CATEGORY_ID && delta > 0 }
        .map { (id, delta) -> Budget(month, id, delta) }
    return (adjusted + added).sortedBy { it.categoryId }
}

/** Số tối đa có thể chuyển đi từ một danh mục: phần hạn mức chưa tiêu. */
fun maxReallocatable(progress: BudgetProgress?): Long = progress?.remaining?.coerceAtLeast(0) ?: 0L

enum class ReallocationError { MISSING_CATEGORY, SAME_CATEGORY, NOT_POSITIVE, EXCEEDS_REMAINING }

fun validateReallocation(fromId: Long?, toId: Long?, amount: Long?, max: Long): ReallocationError? = when {
    fromId == null || toId == null -> ReallocationError.MISSING_CATEGORY
    fromId == toId -> ReallocationError.SAME_CATEGORY
    amount == null || amount <= 0 -> ReallocationError.NOT_POSITIVE
    amount > max -> ReallocationError.EXCEEDS_REMAINING
    else -> null
}
