package com.funmanager.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class GoalProgress(val saved: Long, val target: Long) {
    val ratio: Float get() = if (target <= 0) 0f else (saved.toFloat() / target).coerceIn(0f, 1f)
    val remaining: Long get() = (target - saved).coerceAtLeast(0)
    val reached: Boolean get() = target > 0 && saved >= target
}

/**
 * Số tiền cần để dành mỗi tháng để kịp [deadline], tính cả tháng hiện tại.
 * null nếu không có hạn hoặc đã đạt mục tiêu. Nếu đã quá hạn thì trả về toàn bộ phần còn thiếu.
 */
fun monthlyNeeded(progress: GoalProgress, today: LocalDate, deadline: LocalDate?): Long? {
    if (deadline == null || progress.reached) return null
    val months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline)) + 1
    if (months <= 0) return progress.remaining
    return (progress.remaining + months - 1) / months
}

/** Kiểm tra số tiền rút: phải > 0 và không vượt số đã để dành. */
fun canWithdraw(amount: Long, saved: Long): Boolean = amount in 1..saved
