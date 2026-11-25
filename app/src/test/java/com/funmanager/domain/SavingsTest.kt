package com.funmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SavingsTest {
    private val today = LocalDate.of(2026, 11, 25)

    @Test
    fun progressRatioAndRemaining() {
        val p = GoalProgress(saved = 3_000_000, target = 10_000_000)
        assertEquals(0.3f, p.ratio, 0.0001f)
        assertEquals(7_000_000L, p.remaining)
        assertFalse(p.reached)
        assertTrue(GoalProgress(saved = 12_000_000, target = 10_000_000).reached)
        assertEquals(1f, GoalProgress(saved = 12_000_000, target = 10_000_000).ratio, 0f)
        assertEquals(0L, GoalProgress(saved = 12_000_000, target = 10_000_000).remaining)
    }

    @Test
    fun monthlyNeededCountsCurrentMonthAndRoundsUp() {
        val p = GoalProgress(saved = 0, target = 10_000_000)
        // Tháng 11, 12, 1 → 3 tháng.
        assertEquals(3_333_334L, monthlyNeeded(p, today, LocalDate.of(2027, 1, 31)))
        // Hạn ngay trong tháng này.
        assertEquals(10_000_000L, monthlyNeeded(p, today, LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun monthlyNeededEdgeCases() {
        val p = GoalProgress(saved = 4_000_000, target = 10_000_000)
        assertNull(monthlyNeeded(p, today, null))
        assertNull(monthlyNeeded(GoalProgress(10_000_000, 10_000_000), today, LocalDate.of(2027, 1, 1)))
        // Quá hạn → cần toàn bộ phần còn thiếu.
        assertEquals(6_000_000L, monthlyNeeded(p, today, LocalDate.of(2026, 6, 1)))
    }

    @Test
    fun withdrawLimits() {
        assertTrue(canWithdraw(500_000, 1_000_000))
        assertTrue(canWithdraw(1_000_000, 1_000_000))
        assertFalse(canWithdraw(1_000_001, 1_000_000))
        assertFalse(canWithdraw(0, 1_000_000))
    }
}
