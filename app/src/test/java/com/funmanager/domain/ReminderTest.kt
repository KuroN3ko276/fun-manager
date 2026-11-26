package com.funmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderTest {
    private val nine = LocalTime.of(21, 0)

    private fun allowance(tomorrow: Long? = 180_000, overBy: Long = 0) = DailyAllowance(
        budget = 6_000_000,
        upcomingRecurring = 0,
        perDay = 200_000,
        spentToday = 20_000,
        daysLeft = 10,
        tomorrowPerDay = tomorrow,
        overBy = overBy,
    )

    @Test
    fun remindsTodayBeforeTime() {
        assertEquals(
            LocalDateTime.of(2026, 11, 26, 21, 0),
            nextReminderAt(LocalDateTime.of(2026, 11, 26, 8, 30), nine),
        )
    }

    @Test
    fun remindsTomorrowAtOrAfterTime() {
        assertEquals(LocalDateTime.of(2026, 11, 27, 21, 0), nextReminderAt(LocalDateTime.of(2026, 11, 26, 21, 0), nine))
        assertEquals(LocalDateTime.of(2026, 11, 27, 21, 0), nextReminderAt(LocalDateTime.of(2026, 11, 26, 23, 59), nine))
    }

    @Test
    fun rollsOverMonthAndYear() {
        assertEquals(LocalDateTime.of(2027, 1, 1, 21, 0), nextReminderAt(LocalDateTime.of(2026, 12, 31, 22, 0), nine))
    }

    @Test
    fun bodyKinds() {
        assertEquals(ReminderBody.NOTHING_LOGGED, reminderBody(0, allowance()))
        assertEquals(ReminderBody.NOTHING_LOGGED, reminderBody(0, null))
        assertEquals(ReminderBody.NO_BUDGET, reminderBody(2, null))
        assertEquals(ReminderBody.TOMORROW_ALLOWANCE, reminderBody(2, allowance()))
        assertEquals(ReminderBody.MONTH_OVER, reminderBody(2, allowance(overBy = 1)))
        assertEquals(ReminderBody.LAST_DAY, reminderBody(2, allowance(tomorrow = null)))
    }
}
