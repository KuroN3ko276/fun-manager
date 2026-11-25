package com.funmanager.domain

import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType.EXPENSE
import com.funmanager.data.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MonthlySummaryTest {
    private val food = Category(1, "Ăn uống", "food", 0, EXPENSE)
    private val transport = Category(2, "Đi lại", "transport", 0, EXPENSE)
    private val salary = Category(3, "Lương", "salary", 0, INCOME)
    private val day = LocalDate.of(2026, 11, 1)

    private fun tx(amount: Long, category: Category) =
        Transaction(amount = amount, type = category.type, categoryId = category.id, date = day)

    @Test
    fun totalsIncomeExpenseAndBalance() {
        val s = summarize(
            listOf(tx(10_000_000, salary), tx(300_000, food), tx(200_000, food), tx(500_000, transport)),
            listOf(food, transport, salary),
        )
        assertEquals(10_000_000L, s.income)
        assertEquals(1_000_000L, s.expense)
        assertEquals(9_000_000L, s.balance)
    }

    @Test
    fun groupsExpensesByCategorySortedDescending() {
        val s = summarize(
            listOf(tx(300_000, food), tx(200_000, food), tx(600_000, transport)),
            listOf(food, transport, salary),
        )
        assertEquals(listOf(transport, food), s.expenseByCategory.map { it.category })
        assertEquals(500_000L, s.expenseByCategory[1].amount)
        assertEquals(600_000f / 1_100_000f, s.expenseByCategory[0].share, 0.0001f)
    }

    @Test
    fun emptyMonthHasZeroes() {
        val s = summarize(emptyList(), listOf(food))
        assertEquals(0L, s.balance)
        assertEquals(emptyList<CategoryTotal>(), s.expenseByCategory)
    }
}

class PercentChangeTest {
    @Test
    fun computesRoundedChange() {
        assertEquals(20, percentChange(1_200_000, 1_000_000))
        assertEquals(-25, percentChange(750_000, 1_000_000))
        assertEquals(0, percentChange(1_000_000, 1_000_000))
        assertEquals(33, percentChange(400, 300))
    }

    @Test
    fun nullWhenNoPreviousData() {
        assertEquals(null, percentChange(500_000, 0))
    }
}
