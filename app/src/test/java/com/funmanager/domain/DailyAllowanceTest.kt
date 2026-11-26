package com.funmanager.domain

import com.funmanager.data.Budget
import com.funmanager.data.RecurringRule
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DailyAllowanceTest {
    private val nov = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 21) // còn 10 ngày kể cả hôm nay
    private val food = 1L
    private val fun_ = 2L
    private val rent = 3L

    private fun spend(amount: Long, day: Int, category: Long = food) =
        Transaction(amount = amount, type = TransactionType.EXPENSE, categoryId = category, date = nov.atDay(day))

    private fun income(amount: Long, day: Int) =
        Transaction(amount = amount, type = TransactionType.INCOME, categoryId = 9, date = nov.atDay(day))

    private fun rule(amount: Long, day: Int, next: YearMonth = nov, category: Long = rent, active: Boolean = true, type: TransactionType = TransactionType.EXPENSE) =
        RecurringRule(amount = amount, type = type, categoryId = category, dayOfMonth = day, nextMonth = next, active = active)

    private val total = Budget(nov, Budget.TOTAL_CATEGORY_ID, 6_000_000)

    @Test
    fun splitsRemainingBudgetOverDaysLeftIncludingToday() {
        val result = dailyAllowance(today, listOf(total), listOf(spend(4_000_000, 5)), emptyList())!!
        assertEquals(10, result.daysLeft)
        assertEquals(200_000L, result.perDay)
        assertEquals(200_000L, result.leftToday)
    }

    @Test
    fun spendingTodayReducesLeftTodayButNotPerDay() {
        val txs = listOf(spend(4_000_000, 5), spend(50_000, 21))
        val result = dailyAllowance(today, listOf(total), txs, emptyList())!!
        assertEquals(200_000L, result.perDay)
        assertEquals(50_000L, result.spentToday)
        assertEquals(150_000L, result.leftToday)
    }

    @Test
    fun tomorrowRecalculatesWithTodaysSpending() {
        val txs = listOf(spend(4_000_000, 5), spend(380_000, 21))
        val result = dailyAllowance(today, listOf(total), txs, emptyList())!!
        // (2.000.000 − 380.000) / 9 = 180.000
        assertEquals(180_000L, result.tomorrowPerDay)
    }

    @Test
    fun noTomorrowOnLastDayOfMonth() {
        val result = dailyAllowance(nov.atEndOfMonth(), listOf(total), emptyList(), emptyList())!!
        assertEquals(1, result.daysLeft)
        assertNull(result.tomorrowPerDay)
    }

    @Test
    fun subtractsUpcomingRecurringNotYetCreated() {
        val rules = listOf(
            rule(1_000_000, day = 28),
            rule(300_000, day = 10, next = nov.plusMonths(1)), // đã tạo tháng này
            rule(500_000, day = 25, active = false),
            rule(9_000_000, day = 25, type = TransactionType.INCOME),
        )
        val result = dailyAllowance(today, listOf(total), listOf(spend(4_000_000, 5)), rules)!!
        assertEquals(1_000_000L, result.upcomingRecurring)
        assertEquals(100_000L, result.perDay)
    }

    @Test
    fun futureDatedExpensesAreReservedNotSpentToday() {
        val txs = listOf(spend(4_000_000, 5), spend(1_000_000, 30))
        val result = dailyAllowance(today, listOf(total), txs, emptyList())!!
        assertEquals(0L, result.spentToday)
        assertEquals(100_000L, result.perDay)
    }

    @Test
    fun ignoresIncome() {
        val txs = listOf(spend(4_000_000, 5), income(20_000_000, 1))
        assertEquals(200_000L, dailyAllowance(today, listOf(total), txs, emptyList())!!.perDay)
    }

    @Test
    fun roundsDownToThousand() {
        val result = dailyAllowance(today, listOf(Budget(nov, Budget.TOTAL_CATEGORY_ID, 1_234_567)), emptyList(), emptyList())!!
        assertEquals(123_000L, result.perDay)
    }

    @Test
    fun overBudgetReportsOverAmount() {
        val result = dailyAllowance(today, listOf(total), listOf(spend(6_500_000, 5)), emptyList())!!
        assertEquals(500_000L, result.overBy)
        assertEquals(0L, result.perDay)
    }

    @Test
    fun fallsBackToSumOfCategoryBudgetsAndOnlyCountsThoseCategories() {
        val budgets = listOf(Budget(nov, food, 2_000_000), Budget(nov, fun_, 1_000_000))
        val txs = listOf(spend(1_000_000, 5, food), spend(5_000_000, 6, category = 7))
        val rules = listOf(rule(800_000, day = 28, category = rent))
        val result = dailyAllowance(today, budgets, txs, rules)!!
        assertEquals(3_000_000L, result.budget)
        assertEquals(0L, result.upcomingRecurring)
        assertEquals(200_000L, result.perDay)
    }

    @Test
    fun nullWithoutAnyBudget() {
        assertNull(dailyAllowance(today, emptyList(), listOf(spend(100_000, 5)), emptyList()))
    }
}
