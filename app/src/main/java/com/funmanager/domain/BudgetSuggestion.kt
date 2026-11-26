package com.funmanager.domain

import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import java.time.YearMonth

/** Hạn mức gợi ý dựa trên mức chi trung bình các tháng trước. */
data class BudgetSuggestion(
    /** Số tháng có dữ liệu đã dùng để tính trung bình (1..[SUGGESTION_MONTHS]). */
    val months: Int,
    /** Hạn mức gợi ý theo danh mục chi. */
    val byCategory: Map<Long, Long>,
) {
    /** Ngân sách tổng gợi ý = tổng các danh mục. */
    val total: Long get() = byCategory.values.sum()
}

const val SUGGESTION_MONTHS = 3
private const val ROUND_TO = 10_000L

/**
 * Gợi ý hạn mức cho [month] từ chi tiêu của tối đa [SUGGESTION_MONTHS] tháng trọn vẹn liền trước.
 * Chỉ chia cho số tháng có giao dịch (thu hoặc chi), để người mới dùng app không bị gợi ý quá thấp.
 * Làm tròn lên bội số 10.000 ₫. Trả về null nếu chưa có dữ liệu.
 */
fun suggestBudgets(transactions: List<Transaction>, month: YearMonth): BudgetSuggestion? {
    val from = month.minusMonths(SUGGESTION_MONTHS.toLong())
    val history = transactions.filter { YearMonth.from(it.date).let { m -> m >= from && m < month } }
    val months = history.mapTo(HashSet()) { YearMonth.from(it.date) }.size
    if (months == 0) return null
    val byCategory = history
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.categoryId }
        .mapValues { (_, txs) -> roundUp(ceilDiv(txs.sumOf { it.amount }, months.toLong())) }
        .filterValues { it > 0 }
    return BudgetSuggestion(months, byCategory)
}

/** Giảm gợi ý đi [percent]% (để tiết kiệm), làm tròn lên bội số 10.000 ₫. */
fun reduceSuggestion(amount: Long, percent: Int): Long = roundUp(ceilDiv(amount * (100 - percent), 100))

private fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b

private fun roundUp(amount: Long): Long = ceilDiv(amount, ROUND_TO) * ROUND_TO
