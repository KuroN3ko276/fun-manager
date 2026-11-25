package com.funmanager.domain

import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import java.time.YearMonth

data class MonthTotals(val month: YearMonth, val income: Long, val expense: Long) {
    val balance: Long get() = income - expense
}

/** Danh sách [count] tháng liên tiếp, kết thúc ở [end] (tăng dần). */
fun monthsEndingAt(end: YearMonth, count: Int): List<YearMonth> =
    (count - 1 downTo 0).map { end.minusMonths(it.toLong()) }

/** Tổng thu/chi cho từng tháng trong [months]; tháng không có giao dịch có giá trị 0. */
fun monthlyTotals(transactions: List<Transaction>, months: List<YearMonth>): List<MonthTotals> {
    val byMonth = transactions.groupBy { YearMonth.from(it.date) }
    return months.map { month ->
        val items = byMonth[month].orEmpty()
        MonthTotals(
            month = month,
            income = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            expense = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        )
    }
}

/** Chi tiêu trung bình mỗi tháng, chỉ tính những tháng có phát sinh giao dịch. */
fun averageExpense(totals: List<MonthTotals>): Long {
    val active = totals.filter { it.income > 0 || it.expense > 0 }
    return if (active.isEmpty()) 0 else active.sumOf { it.expense } / active.size
}
