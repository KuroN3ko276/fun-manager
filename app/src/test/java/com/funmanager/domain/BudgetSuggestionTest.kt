package com.funmanager.domain

import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class BudgetSuggestionTest {
    private val dec = YearMonth.of(2026, 12)
    private val nov = YearMonth.of(2026, 11)
    private val oct = YearMonth.of(2026, 10)
    private val sep = YearMonth.of(2026, 9)
    private val aug = YearMonth.of(2026, 8)
    private val food = 1L
    private val fun_ = 2L

    private fun spend(amount: Long, month: YearMonth, category: Long = food, day: Int = 10) =
        Transaction(amount = amount, type = TransactionType.EXPENSE, categoryId = category, date = month.atDay(day))

    private fun income(amount: Long, month: YearMonth) =
        Transaction(amount = amount, type = TransactionType.INCOME, categoryId = 9, date = month.atDay(1))

    @Test
    fun averagesLastThreeFullMonthsAndRoundsUpToTenThousand() {
        val txs = listOf(spend(3_000_000, sep), spend(3_100_000, oct), spend(2_905_000, nov))
        val s = suggestBudgets(txs, dec)!!
        assertEquals(3, s.months)
        assertEquals(3_010_000L, s.byCategory[food]) // 3.001.666 → 3.010.000
    }

    @Test
    fun ignoresCurrentMonthAndOlderMonths() {
        val txs = listOf(spend(3_000_000, nov), spend(9_000_000, dec), spend(9_000_000, aug))
        val s = suggestBudgets(txs, dec)!!
        assertEquals(3_000_000L, s.byCategory[food])
    }

    @Test
    fun onlyCountsMonthsThatHaveData() {
        // Mới dùng app từ tháng 11: không chia cho 3.
        val txs = listOf(spend(1_200_000, nov), income(10_000_000, nov))
        val s = suggestBudgets(txs, dec)!!
        assertEquals(1, s.months)
        assertEquals(1_200_000L, s.byCategory[food])
    }

    @Test
    fun monthWithOnlyIncomeStillCountsAsData() {
        val txs = listOf(income(10_000_000, oct), spend(600_000, nov, fun_))
        val s = suggestBudgets(txs, dec)!!
        assertEquals(2, s.months)
        assertEquals(300_000L, s.byCategory[fun_])
    }

    @Test
    fun totalIsSumOfCategoryAverages() {
        val txs = listOf(spend(2_000_000, nov, food), spend(505_000, nov, fun_))
        val s = suggestBudgets(txs, dec)!!
        assertEquals(2_510_000L, s.total) // 2.000.000 + 510.000
    }

    @Test
    fun nullWhenNoHistory() {
        assertNull(suggestBudgets(listOf(spend(1_000_000, dec)), dec))
    }

    @Test
    fun reduceByPercentRoundsUp() {
        assertEquals(2_710_000L, reduceSuggestion(3_010_000, 10)) // 2.709.000 → 2.710.000
        assertEquals(3_010_000L, reduceSuggestion(3_010_000, 0))
    }
}
