package com.funmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RecurringTest {
    @Test
    fun clampsDayToEndOfShortMonths() {
        assertEquals(LocalDate.of(2026, 2, 28), occurrenceDate(YearMonth.of(2026, 2), 31))
        assertEquals(LocalDate.of(2028, 2, 29), occurrenceDate(YearMonth.of(2028, 2), 30))
        assertEquals(LocalDate.of(2026, 4, 30), occurrenceDate(YearMonth.of(2026, 4), 31))
    }

    @Test
    fun firstMonthIsThisMonthIfDayNotPassed() {
        val today = LocalDate.of(2026, 11, 25)
        assertEquals(YearMonth.of(2026, 11), firstMonthFrom(25, today))
        assertEquals(YearMonth.of(2026, 11), firstMonthFrom(30, today))
        assertEquals(YearMonth.of(2026, 12), firstMonthFrom(5, today))
    }

    @Test
    fun dueOccurrencesCatchesUpMissedMonths() {
        val due = dueOccurrences(5, YearMonth.of(2026, 9), LocalDate.of(2026, 11, 25))
        assertEquals(
            listOf(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 5)),
            due.dates,
        )
        assertEquals(YearMonth.of(2026, 12), due.nextMonth)
    }

    @Test
    fun nothingDueBeforeTheDay() {
        val due = dueOccurrences(28, YearMonth.of(2026, 11), LocalDate.of(2026, 11, 25))
        assertEquals(emptyList<LocalDate>(), due.dates)
        assertEquals(YearMonth.of(2026, 11), due.nextMonth)
    }

    @Test
    fun dueOnTheExactDay() {
        val due = dueOccurrences(25, YearMonth.of(2026, 11), LocalDate.of(2026, 11, 25))
        assertEquals(listOf(LocalDate.of(2026, 11, 25)), due.dates)
    }

    @Test
    fun resumeNeverRegeneratesOrBackfills() {
        val today = LocalDate.of(2026, 11, 25)
        // Đã tạo tháng 11, đổi sang ngày 28 → vẫn bắt đầu từ tháng 12.
        assertEquals(YearMonth.of(2026, 12), resumeMonth(YearMonth.of(2026, 12), 28, today))
        // Tạm dừng từ tháng 8, bật lại → không tạo bù 8, 9, 10.
        assertEquals(YearMonth.of(2026, 12), resumeMonth(YearMonth.of(2026, 8), 5, today))
        // Quy tắc mới.
        assertEquals(YearMonth.of(2026, 11), resumeMonth(null, 30, today))
    }
}
