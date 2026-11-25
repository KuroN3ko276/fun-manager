package com.funmanager.domain

import com.funmanager.data.Budget
import com.funmanager.data.Category
import com.funmanager.data.TransactionType.EXPENSE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class WidgetLinesTest {
    private val food = Category(1, "Ăn uống", "food", 0, EXPENSE)
    private val bill = Category(2, "Hoá đơn", "bill", 0, EXPENSE)
    private val entertainment = Category(3, "Giải trí", "entertainment", 0, EXPENSE)
    private val shop = Category(4, "Mua sắm", "shopping", 0, EXPENSE)
    private val other = Category(5, "Khác", "other", 0, EXPENSE)
    private val cats = listOf(food, bill, entertainment, shop, other)
    private val month = YearMonth.of(2026, 11)
    private val spent = mapOf(1L to 2_500_000L, 2L to 800_000L, 3L to 1_200_000L)
    private val budgets = listOf(
        Budget(month, Budget.TOTAL_CATEGORY_ID, 10_000_000),
        Budget(month, 1, 3_000_000),
        Budget(month, 3, 1_000_000),
    )

    @Test
    fun selectedCategoriesInOrderWithRemaining() {
        val lines = widgetCategoryLines(listOf(3, 1, 2), cats, spent, budgets)
        assertEquals(listOf(entertainment, food, bill), lines.map { it.category })
        assertEquals(-200_000L, lines[0].budget?.remaining)
        assertEquals(BudgetLevel.EXCEEDED, lines[0].budget?.level)
        assertEquals(500_000L, lines[1].budget?.remaining)
        assertNull(lines[2].budget)
        assertEquals(800_000L, lines[2].spent)
    }

    @Test
    fun defaultsToBudgetedCategoriesByMostSpent() {
        val lines = widgetCategoryLines(null, cats, spent, budgets)
        assertEquals(listOf(food, entertainment), lines.map { it.category })
    }

    @Test
    fun dropsDeletedCategoriesAndCapsAtFour() {
        assertEquals(listOf(food), widgetCategoryLines(listOf(99, 1), cats, spent, budgets).map { it.category })
        assertEquals(4, widgetCategoryLines(listOf(1, 2, 3, 4, 5), cats, spent, budgets).size)
        assertEquals(0L, widgetCategoryLines(listOf(4), cats, spent, budgets)[0].spent)
    }
}
