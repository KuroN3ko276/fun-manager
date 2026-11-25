package com.funmanager.domain

import com.funmanager.data.Budget
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class BudgetCarryOverTest {
    private val sep = YearMonth.of(2026, 9)
    private val oct = YearMonth.of(2026, 10)
    private val nov = YearMonth.of(2026, 11)
    private val dec = YearMonth.of(2026, 12)
    private val food = 1L

    private fun limitOf(all: List<Budget>, month: YearMonth, categoryId: Long) =
        effectiveBudgets(all, month).firstOrNull { it.categoryId == categoryId }?.limit

    @Test
    fun limitCarriesForwardUntilChanged() {
        val all = listOf(Budget(sep, food, 3_000_000), Budget(nov, food, 2_500_000))
        assertEquals(null, limitOf(all, YearMonth.of(2026, 8), food))
        assertEquals(3_000_000L, limitOf(all, sep, food))
        assertEquals(3_000_000L, limitOf(all, oct, food))
        assertEquals(2_500_000L, limitOf(all, nov, food))
        assertEquals(2_500_000L, limitOf(all, YearMonth.of(2027, 6), food))
    }

    @Test
    fun keepsStartMonthOfInheritedLimit() {
        val result = effectiveBudgets(listOf(Budget(sep, food, 3_000_000)), dec)
        assertEquals(listOf(Budget(sep, food, 3_000_000)), result)
    }

    @Test
    fun zeroLimitStopsCarryOver() {
        val all = listOf(Budget(sep, food, 3_000_000), Budget(nov, food, 0))
        assertEquals(3_000_000L, limitOf(all, oct, food))
        assertEquals(null, limitOf(all, nov, food))
        assertEquals(null, limitOf(all, dec, food))
    }

    @Test
    fun totalAndCategoriesAreIndependent() {
        val all = listOf(Budget(sep, Budget.TOTAL_CATEGORY_ID, 10_000_000), Budget(oct, food, 3_000_000))
        assertEquals(listOf(Budget.TOTAL_CATEGORY_ID, food), effectiveBudgets(all, nov).map { it.categoryId })
    }

    @Test
    fun clearingInheritedLimitWritesZeroMarker() {
        val all = listOf(Budget(sep, food, 3_000_000))
        assertEquals(BudgetChange.Upsert(Budget(nov, food, 0)), budgetChange(all, nov, food, null))
    }

    @Test
    fun clearingLimitSetThisMonthDeletesRowWhenNothingEarlier() {
        val all = listOf(Budget(nov, food, 3_000_000))
        assertEquals(BudgetChange.Delete(nov, food), budgetChange(all, nov, food, null))
    }

    @Test
    fun settingLimitUpserts() {
        assertEquals(BudgetChange.Upsert(Budget(nov, food, 2_000_000)), budgetChange(emptyList(), nov, food, 2_000_000))
    }
}
