package com.funmanager.domain

import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType.EXPENSE
import com.funmanager.data.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TransactionFilterTest {
    private val food = Category(1, "Ăn uống", "food", 0, EXPENSE)
    private val bill = Category(2, "Hoá đơn", "bill", 0, EXPENSE)
    private val salary = Category(3, "Lương", "salary", 0, INCOME)
    private val cats = listOf(food, bill, salary).associateBy { it.id }
    private val today = LocalDate.of(2026, 11, 25)

    private val pho = Transaction(1, 45_000, EXPENSE, 1, LocalDate.of(2026, 11, 20), "Phở bò")
    private val net = Transaction(2, 250_000, EXPENSE, 2, LocalDate.of(2026, 10, 11), "Tiền mạng Viettel", walletId = 2)
    private val luong = Transaction(3, 15_000_000, INCOME, 3, LocalDate.of(2026, 11, 10), "")
    private val old = Transaction(4, 120_000, EXPENSE, 1, LocalDate.of(2025, 12, 31), "Đồ ăn tất niên")
    private val all = listOf(pho, net, luong, old)

    private fun ids(filter: TransactionFilter) = filterTransactions(all, cats, filter, today).map { it.id }

    @Test
    fun normalizesVietnamese() {
        assertEquals("an uong", normalizeForSearch("  Ăn Uống "))
        assertEquals("do an tat nien", normalizeForSearch("Đồ ăn tất niên"))
    }

    @Test
    fun queryMatchesNoteOrCategoryWithoutDiacritics() {
        assertEquals(listOf(1L, 4L), ids(TransactionFilter(query = "an uong")))
        assertEquals(listOf(1L), ids(TransactionFilter(query = "pho")))
        assertEquals(listOf(3L), ids(TransactionFilter(query = "LUONG")))
        assertEquals(listOf(2L), ids(TransactionFilter(query = "mang viettel")))
        assertEquals(emptyList<Long>(), ids(TransactionFilter(query = "mang vnpt")))
    }

    @Test
    fun filtersByTypeCategoryAndAmount() {
        assertEquals(listOf(3L), ids(TransactionFilter(type = INCOME)))
        assertEquals(listOf(1L, 4L), ids(TransactionFilter(categoryIds = setOf(1))))
        assertEquals(listOf(2L, 4L), ids(TransactionFilter(minAmount = 100_000, maxAmount = 1_000_000)))
    }

    @Test
    fun filtersByDatePresets() {
        assertEquals(listOf(1L, 3L), ids(TransactionFilter(datePreset = DatePreset.THIS_MONTH)))
        assertEquals(listOf(1L, 2L, 3L), ids(TransactionFilter(datePreset = DatePreset.LAST_3_MONTHS)))
        assertEquals(listOf(1L, 2L, 3L), ids(TransactionFilter(datePreset = DatePreset.THIS_YEAR)))
        assertEquals(
            listOf(2L, 4L),
            ids(
                TransactionFilter(
                    datePreset = DatePreset.CUSTOM,
                    customFrom = LocalDate.of(2025, 12, 1),
                    customTo = LocalDate.of(2026, 10, 31),
                ),
            ),
        )
    }

    @Test
    fun filtersByWallet() {
        assertEquals(listOf(2L), ids(TransactionFilter(walletIds = setOf(2))))
        assertEquals(listOf(1L, 3L, 4L), ids(TransactionFilter(walletIds = setOf(1))))
    }

    @Test
    fun countsActiveFilters() {
        assertEquals(0, TransactionFilter(query = "x").activeCount)
        assertEquals(2, TransactionFilter(type = EXPENSE, minAmount = 1).activeCount)
    }
}
