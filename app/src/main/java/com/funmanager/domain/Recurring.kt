package com.funmanager.domain

import java.time.LocalDate
import java.time.YearMonth

/** Ngày phát sinh trong [month]; nếu tháng ngắn hơn [dayOfMonth] thì lấy ngày cuối tháng. */
fun occurrenceDate(month: YearMonth, dayOfMonth: Int): LocalDate =
    month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))

/** Tháng đầu tiên có lần phát sinh vào hoặc sau [today]. */
fun firstMonthFrom(dayOfMonth: Int, today: LocalDate): YearMonth {
    val current = YearMonth.from(today)
    return if (occurrenceDate(current, dayOfMonth) >= today) current else current.plusMonths(1)
}

/** Kết quả tính các lần phát sinh đã đến hạn. */
data class DueOccurrences(val dates: List<LocalDate>, val nextMonth: YearMonth)

/** Các ngày đã đến hạn (<= [today]) bắt đầu từ [nextMonth], và tháng kế tiếp sau đó. */
fun dueOccurrences(dayOfMonth: Int, nextMonth: YearMonth, today: LocalDate): DueOccurrences {
    val dates = mutableListOf<LocalDate>()
    var month = nextMonth
    while (occurrenceDate(month, dayOfMonth) <= today) {
        dates += occurrenceDate(month, dayOfMonth)
        month = month.plusMonths(1)
    }
    return DueOccurrences(dates, month)
}

/**
 * Tháng kế tiếp khi lưu/bật lại một quy tắc: không tạo bù những lần đã qua,
 * và không tạo lại tháng đã tạo rồi.
 */
fun resumeMonth(currentNext: YearMonth?, dayOfMonth: Int, today: LocalDate): YearMonth {
    val first = firstMonthFrom(dayOfMonth, today)
    return if (currentNext != null && currentNext > first) currentNext else first
}
