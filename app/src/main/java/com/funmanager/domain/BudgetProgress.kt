package com.funmanager.domain

enum class BudgetLevel { OK, WARNING, EXCEEDED }

data class BudgetProgress(val spent: Long, val limit: Long) {
    /** Tỷ lệ đã chi trên hạn mức, có thể > 1 khi vượt. */
    val ratio: Float get() = if (limit <= 0) 0f else spent.toFloat() / limit

    val remaining: Long get() = limit - spent

    val level: BudgetLevel
        get() = when {
            limit <= 0 -> BudgetLevel.OK
            spent > limit -> BudgetLevel.EXCEEDED
            spent * 100 >= limit * WARNING_PERCENT -> BudgetLevel.WARNING
            else -> BudgetLevel.OK
        }

    companion object {
        const val WARNING_PERCENT = 80
    }
}
