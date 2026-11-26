package com.funmanager.domain

import com.funmanager.data.Budget
import com.funmanager.data.RecurringRule
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/** Số được tiêu mỗi ngày để không vượt ngân sách tháng. */
data class DailyAllowance(
    /** Hạn mức dùng để tính: ngân sách tổng, hoặc tổng hạn mức các danh mục nếu chưa đặt ngân sách tổng. */
    val budget: Long,
    /** Khoản chi định kỳ còn lại trong tháng (chưa được tạo), đã được để dành ra. */
    val upcomingRecurring: Long,
    /** Được tiêu mỗi ngày, tính từ đầu hôm nay; giữ nguyên trong ngày. 0 khi đã hết ngân sách. */
    val perDay: Long,
    val spentToday: Long,
    /** Số ngày còn lại trong tháng, kể cả hôm nay. */
    val daysLeft: Int,
    /** Mức mỗi ngày từ ngày mai nếu hôm nay dừng ở [spentToday]; null nếu hôm nay là ngày cuối tháng. */
    val tomorrowPerDay: Long?,
    /** Số đã vượt ngân sách tháng (tính cả định kỳ sắp tới), 0 nếu chưa vượt. */
    val overBy: Long,
) {
    /** Hôm nay còn được tiêu; âm khi đã tiêu quá mức của ngày. */
    val leftToday: Long get() = perDay - spentToday
}

/**
 * Tính số được tiêu mỗi ngày cho tháng chứa [today].
 *
 * [budgets] là hạn mức đang áp dụng của tháng, [transactions] là giao dịch của tháng.
 * Khoản chi ghi ngày tương lai và khoản định kỳ chưa tạo được trừ trước như tiền đã cam kết.
 * Trả về null nếu chưa đặt ngân sách nào.
 */
fun dailyAllowance(
    today: LocalDate,
    budgets: List<Budget>,
    transactions: List<Transaction>,
    recurring: List<RecurringRule>,
): DailyAllowance? {
    val month = YearMonth.from(today)
    val total = budgets.firstOrNull { it.categoryId == Budget.TOTAL_CATEGORY_ID }?.limit
    val categoryLimits = budgets.filter { it.categoryId != Budget.TOTAL_CATEGORY_ID }
    // Không có ngân sách tổng: chỉ tính các danh mục đã đặt hạn mức.
    val counts: (Long) -> Boolean
    val budget: Long
    if (total != null) {
        budget = total
        counts = { true }
    } else {
        if (categoryLimits.isEmpty()) return null
        budget = categoryLimits.sumOf { it.limit }
        val ids = categoryLimits.mapTo(HashSet()) { it.categoryId }
        counts = { it in ids }
    }

    val expenses = transactions.filter {
        it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month && counts(it.categoryId)
    }
    val spentBefore = expenses.filter { it.date < today }.sumOf { it.amount }
    val spentToday = expenses.filter { it.date == today }.sumOf { it.amount }
    val reserved = expenses.filter { it.date > today }.sumOf { it.amount }
    val upcoming = recurring
        .filter { it.active && it.type == TransactionType.EXPENSE && it.nextMonth == month && counts(it.categoryId) }
        .sumOf { it.amount }

    val daysLeft = month.lengthOfMonth() - today.dayOfMonth + 1
    val pool = budget - spentBefore - reserved - upcoming
    val tomorrowPool = pool - spentToday
    return DailyAllowance(
        budget = budget,
        upcomingRecurring = upcoming,
        perDay = roundDownThousand(pool / daysLeft).coerceAtLeast(0),
        spentToday = spentToday,
        daysLeft = daysLeft,
        tomorrowPerDay = if (daysLeft > 1) roundDownThousand(tomorrowPool / (daysLeft - 1)) else null,
        overBy = (-tomorrowPool).coerceAtLeast(0),
    )
}

/** Làm tròn xuống bội số 1.000 ₫ cho số dương (để con số dễ nhớ và không vượt). */
private fun roundDownThousand(amount: Long): Long = if (amount > 0) amount / 1_000 * 1_000 else amount
