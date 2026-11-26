package com.funmanager.domain

import com.funmanager.data.Budget
import com.funmanager.data.BudgetAdjustment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class BudgetReallocationTest {
    private val sep = YearMonth.of(2026, 9)
    private val nov = YearMonth.of(2026, 11)
    private val dec = YearMonth.of(2026, 12)
    private val food = 1L
    private val fun_ = 2L
    private val health = 3L

    private val base = listOf(
        Budget(sep, Budget.TOTAL_CATEGORY_ID, 5_000_000),
        Budget(sep, food, 3_000_000),
        Budget(sep, fun_, 1_000_000),
    )

    private fun limits(budgets: List<Budget>) = budgets.associate { it.categoryId to it.limit }

    private fun move(from: Long, to: Long, amount: Long, month: YearMonth = nov) =
        BudgetAdjustment(month = month, fromCategoryId = from, toCategoryId = to, amount = amount)

    @Test
    fun movesLimitBetweenCategoriesInMonth() {
        val result = applyAdjustments(effectiveBudgets(base, nov), listOf(move(fun_, food, 500_000)), nov)
        assertEquals(3_500_000L, limits(result)[food])
        assertEquals(500_000L, limits(result)[fun_])
    }

    @Test
    fun totalBudgetIsUnchanged() {
        val result = applyAdjustments(effectiveBudgets(base, nov), listOf(move(fun_, food, 500_000)), nov)
        assertEquals(5_000_000L, limits(result)[Budget.TOTAL_CATEGORY_ID])
    }

    @Test
    fun keepsStartMonthOfInheritedLimit() {
        val result = applyAdjustments(effectiveBudgets(base, nov), listOf(move(fun_, food, 500_000)), nov)
        assertEquals(sep, result.first { it.categoryId == food }.month)
    }

    @Test
    fun targetWithoutLimitGetsReceivedAmountForThatMonth() {
        val result = applyAdjustments(effectiveBudgets(base, nov), listOf(move(fun_, health, 300_000)), nov)
        assertEquals(Budget(nov, health, 300_000), result.first { it.categoryId == health })
        assertEquals(700_000L, limits(result)[fun_])
    }

    @Test
    fun multipleMovesAccumulate() {
        val adjustments = listOf(move(fun_, food, 200_000), move(fun_, food, 300_000), move(food, health, 100_000))
        val result = limits(applyAdjustments(effectiveBudgets(base, nov), adjustments, nov))
        assertEquals(3_400_000L, result[food])
        assertEquals(500_000L, result[fun_])
        assertEquals(100_000L, result[health])
    }

    @Test
    fun otherMonthsAreNotAffected() {
        val adjustments = listOf(move(fun_, food, 500_000, month = nov))
        val result = limits(applyAdjustments(effectiveBudgets(base, dec), adjustments, dec))
        assertEquals(3_000_000L, result[food])
        assertEquals(1_000_000L, result[fun_])
    }

    @Test
    fun clampsAtZeroWhenSourceLimitWasLaterRemoved() {
        val withoutFun = base.filter { it.categoryId != fun_ }
        val result = applyAdjustments(effectiveBudgets(withoutFun, nov), listOf(move(fun_, food, 500_000)), nov)
        assertNull(limits(result)[fun_])
        assertEquals(3_500_000L, limits(result)[food])
    }

    @Test
    fun sourceMovedToZeroStillShowsZeroLimit() {
        val result = applyAdjustments(effectiveBudgets(base, nov), listOf(move(fun_, food, 1_000_000)), nov)
        assertEquals(0L, limits(result)[fun_])
    }

    @Test
    fun netAdjustmentPerCategory() {
        val adjustments = listOf(move(fun_, food, 200_000), move(food, health, 50_000))
        assertEquals(mapOf(fun_ to -200_000L, food to 150_000L, health to 50_000L), netAdjustments(adjustments))
    }

    @Test
    fun maxIsUnspentPartOfLimit() {
        assertEquals(400_000L, maxReallocatable(BudgetProgress(spent = 600_000, limit = 1_000_000)))
        assertEquals(0L, maxReallocatable(BudgetProgress(spent = 1_200_000, limit = 1_000_000)))
        assertEquals(0L, maxReallocatable(null))
    }

    @Test
    fun validation() {
        assertEquals(ReallocationError.SAME_CATEGORY, validateReallocation(food, food, 100, 1_000))
        assertEquals(ReallocationError.NOT_POSITIVE, validateReallocation(food, fun_, 0, 1_000))
        assertEquals(ReallocationError.NOT_POSITIVE, validateReallocation(food, fun_, null, 1_000))
        assertEquals(ReallocationError.EXCEEDS_REMAINING, validateReallocation(food, fun_, 1_001, 1_000))
        assertEquals(ReallocationError.MISSING_CATEGORY, validateReallocation(null, fun_, 100, 1_000))
        assertNull(validateReallocation(food, fun_, 1_000, 1_000))
    }
}
