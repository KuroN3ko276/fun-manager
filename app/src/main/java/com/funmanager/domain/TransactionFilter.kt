package com.funmanager.domain

import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth

enum class DatePreset { THIS_MONTH, LAST_3_MONTHS, THIS_YEAR, ALL, CUSTOM }

data class TransactionFilter(
    val query: String = "",
    /** null = cả thu và chi. */
    val type: TransactionType? = null,
    /** Rỗng = mọi danh mục. */
    val categoryIds: Set<Long> = emptySet(),
    val datePreset: DatePreset = DatePreset.ALL,
    /** Chỉ dùng khi [datePreset] = CUSTOM; null = không giới hạn đầu/cuối. */
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
) {
    val isEmpty: Boolean
        get() = this == TransactionFilter(query = query) && query.isBlank()

    /** Số bộ lọc đang bật (không tính ô tìm kiếm). */
    val activeCount: Int
        get() = listOf(
            type != null,
            categoryIds.isNotEmpty(),
            datePreset != DatePreset.ALL,
            minAmount != null || maxAmount != null,
        ).count { it }

    /** Khoảng ngày thực tế theo [datePreset], tính từ [today]. */
    fun dateRange(today: LocalDate): Pair<LocalDate?, LocalDate?> = when (datePreset) {
        DatePreset.THIS_MONTH -> YearMonth.from(today).let { it.atDay(1) to it.atEndOfMonth() }
        DatePreset.LAST_3_MONTHS -> YearMonth.from(today).let { it.minusMonths(2).atDay(1) to it.atEndOfMonth() }
        DatePreset.THIS_YEAR -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        DatePreset.ALL -> null to null
        DatePreset.CUSTOM -> customFrom to customTo
    }
}

/** Bỏ dấu tiếng Việt và chữ hoa để so khớp: "Ăn Uống" → "an uong". */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd')
        .replace('Đ', 'D')
        .lowercase()
        .trim()

/** Lọc giao dịch; ô tìm kiếm khớp ghi chú hoặc tên danh mục, mọi từ phải có mặt. */
fun filterTransactions(
    transactions: List<Transaction>,
    categories: Map<Long, Category>,
    filter: TransactionFilter,
    today: LocalDate,
): List<Transaction> {
    val words = normalizeForSearch(filter.query).split(Regex("\\s+")).filter { it.isNotEmpty() }
    val (from, to) = filter.dateRange(today)
    return transactions.filter { t ->
        (filter.type == null || t.type == filter.type) &&
            (filter.categoryIds.isEmpty() || t.categoryId in filter.categoryIds) &&
            (from == null || t.date >= from) &&
            (to == null || t.date <= to) &&
            (filter.minAmount == null || t.amount >= filter.minAmount) &&
            (filter.maxAmount == null || t.amount <= filter.maxAmount) &&
            (words.isEmpty() || matchesWords(t, categories[t.categoryId], words))
    }
}

private fun matchesWords(t: Transaction, category: Category?, words: List<String>): Boolean {
    val haystack = normalizeForSearch(t.note + " " + category?.name.orEmpty())
    return words.all { it in haystack }
}
