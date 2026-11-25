package com.funmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Test

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
