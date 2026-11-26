package com.funmanager.domain

import java.time.LocalDateTime
import java.time.LocalTime

/** Lần nhắc kế tiếp lúc [time]: hôm nay nếu chưa tới giờ, ngược lại là ngày mai. */
fun nextReminderAt(now: LocalDateTime, time: LocalTime): LocalDateTime {
    val today = now.toLocalDate().atTime(time)
    return if (today.isAfter(now)) today else today.plusDays(1)
}

/** Nội dung chính của thông báo buổi tối. */
enum class ReminderBody {
    /** Chưa ghi giao dịch nào hôm nay. */
    NOTHING_LOGGED,

    /** Ngày mai được tiêu bao nhiêu. */
    TOMORROW_ALLOWANCE,

    /** Đã vượt ngân sách tháng. */
    MONTH_OVER,

    /** Hôm nay là ngày cuối tháng (không còn "ngày mai" trong tháng). */
    LAST_DAY,

    /** Chưa đặt ngân sách: chỉ tóm tắt số đã chi. */
    NO_BUDGET,
}

fun reminderBody(entriesToday: Int, allowance: DailyAllowance?): ReminderBody = when {
    entriesToday == 0 -> ReminderBody.NOTHING_LOGGED
    allowance == null -> ReminderBody.NO_BUDGET
    allowance.overBy > 0 -> ReminderBody.MONTH_OVER
    allowance.tomorrowPerDay == null -> ReminderBody.LAST_DAY
    else -> ReminderBody.TOMORROW_ALLOWANCE
}

data class ReminderSettings(val enabled: Boolean = false, val time: LocalTime = DEFAULT_TIME) {
    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(21, 0)
    }
}
