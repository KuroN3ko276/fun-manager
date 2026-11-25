package com.funmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class MonthSnapshotTest {
    private val month = YearMonth.of(2026, 11)

    @Test
    fun withoutBudget() {
        val s = MonthSnapshot(month, income = 15_000_000, expense = 4_000_000, totalBudget = null)
        assertEquals(11_000_000L, s.balance)
        assertNull(s.budget)
        assertEquals(0, s.budgetPercent)
    }

    @Test
    fun budgetPercentIsClamped() {
        assertEquals(40, MonthSnapshot(month, 0, 4_000_000, 10_000_000).budgetPercent)
        assertEquals(100, MonthSnapshot(month, 0, 12_000_000, 10_000_000).budgetPercent)
        assertEquals(BudgetLevel.EXCEEDED, MonthSnapshot(month, 0, 12_000_000, 10_000_000).budget?.level)
    }
}
