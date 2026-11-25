package com.funmanager.domain

import com.funmanager.data.Budget
import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class BudgetProgressTest {
    @Test
    fun levels() {
        assertEquals(BudgetLevel.OK, BudgetProgress(spent = 790_000, limit = 1_000_000).level)
        assertEquals(BudgetLevel.WARNING, BudgetProgress(spent = 800_000, limit = 1_000_000).level)
        assertEquals(BudgetLevel.WARNING, BudgetProgress(spent = 1_000_000, limit = 1_000_000).level)
        assertEquals(BudgetLevel.EXCEEDED, BudgetProgress(spent = 1_000_001, limit = 1_000_000).level)
    }

    @Test
    fun ratioAndRemaining() {
        val p = BudgetProgress(spent = 1_500_000, limit = 1_000_000)
        assertEquals(1.5f, p.ratio, 0.0001f)
        assertEquals(-500_000L, p.remaining)
    }

    @Test
    fun zeroLimitIsSafe() {
        assertEquals(0f, BudgetProgress(spent = 100, limit = 0).ratio, 0f)
        assertEquals(BudgetLevel.OK, BudgetProgress(spent = 100, limit = 0).level)
    }
}

class BudgetOverviewTest {
    private val food = Category(1, "Ăn uống", "food", 0, TransactionType.EXPENSE)
    private val entertainment = Category(2, "Giải trí", "entertainment", 0, TransactionType.EXPENSE)
    private val salary = Category(3, "Lương", "salary", 0, TransactionType.INCOME)
    private val month = YearMonth.of(2026, 11)
    private val day = month.atDay(5)

    private fun tx(amount: Long, c: Category) =
        Transaction(amount = amount, type = c.type, categoryId = c.id, date = day)

    @Test
    fun combinesBudgetsWithSpending() {
        val cats = listOf(food, entertainment, salary)
        val summary = summarize(listOf(tx(900_000, food), tx(200_000, entertainment), tx(5_000_000, salary)), cats)
        val overview = budgetOverview(
            summary,
            listOf(
                Budget(month, Budget.TOTAL_CATEGORY_ID, 1_000_000),
                Budget(month, food.id, 1_000_000),
            ),
            cats,
        )
        assertEquals(BudgetLevel.EXCEEDED, overview.total?.level)
        assertEquals(listOf(food, entertainment), overview.categories.map { it.category })
        assertEquals(BudgetLevel.WARNING, overview.categories[0].progress?.level)
        assertEquals(null, overview.categories[1].progress)
        assertEquals(listOf(food), overview.alerts.map { it.category })
        assertEquals(true, overview.hasAlert)
    }

    @Test
    fun noBudgetsMeansNoAlert() {
        val overview = budgetOverview(summarize(listOf(tx(900_000, food)), listOf(food)), emptyList(), listOf(food))
        assertEquals(null, overview.total)
        assertEquals(false, overview.hasAlert)
    }
}
