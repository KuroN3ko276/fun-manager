package com.funmanager.domain

import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import com.funmanager.data.TransactionType.EXPENSE
import com.funmanager.data.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TrendTest {
    private fun tx(amount: Long, type: TransactionType, date: LocalDate) =
        Transaction(amount = amount, type = type, categoryId = 1, date = date)

    @Test
    fun monthsEndingAtCrossesYearBoundary() {
        assertEquals(
            listOf(YearMonth.of(2025, 11), YearMonth.of(2025, 12), YearMonth.of(2026, 1)),
            monthsEndingAt(YearMonth.of(2026, 1), 3),
        )
    }

    @Test
    fun totalsPerMonthIncludingEmptyMonths() {
        val months = monthsEndingAt(YearMonth.of(2026, 11), 3)
        val totals = monthlyTotals(
            listOf(
                tx(10_000_000, INCOME, LocalDate.of(2026, 9, 5)),
                tx(3_000_000, EXPENSE, LocalDate.of(2026, 9, 30)),
                tx(500_000, EXPENSE, LocalDate.of(2026, 11, 1)),
                tx(999, EXPENSE, LocalDate.of(2026, 8, 31)),
            ),
            months,
        )
        assertEquals(listOf(10_000_000L, 0L, 0L), totals.map { it.income })
        assertEquals(listOf(3_000_000L, 0L, 500_000L), totals.map { it.expense })
        assertEquals(7_000_000L, totals[0].balance)
    }

    @Test
    fun averageIgnoresEmptyMonths() {
        val totals = listOf(
            MonthTotals(YearMonth.of(2026, 9), 0, 3_000_000),
            MonthTotals(YearMonth.of(2026, 10), 0, 0),
            MonthTotals(YearMonth.of(2026, 11), 0, 1_000_000),
        )
        assertEquals(2_000_000L, averageExpense(totals))
        assertEquals(0L, averageExpense(emptyList()))
    }
}
