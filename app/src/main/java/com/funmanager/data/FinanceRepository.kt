package com.funmanager.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

class FinanceRepository(private val db: AppDatabase) {
    val categories: Flow<List<Category>> = db.categoryDao().observeAll()

    suspend fun saveCategory(category: Category) = db.categoryDao().upsert(category)

    suspend fun transactionCount(categoryId: Long): Int = db.transactionDao().countForCategory(categoryId)

    /** Xoá danh mục; giao dịch đang dùng nó được chuyển sang [replacementId], ngân sách của nó bị xoá. */
    suspend fun deleteCategory(categoryId: Long, replacementId: Long?) = db.withTransaction {
        if (replacementId != null) db.transactionDao().moveCategory(categoryId, replacementId)
        db.budgetDao().deleteForCategory(categoryId)
        db.categoryDao().delete(categoryId)
    }

    fun transactionsIn(month: YearMonth): Flow<List<Transaction>> =
        db.transactionDao().observeBetween(month.atDay(1), month.atEndOfMonth())

    /** Giao dịch từ đầu tháng [from] đến hết tháng [to]. */
    fun transactionsBetween(from: YearMonth, to: YearMonth): Flow<List<Transaction>> =
        db.transactionDao().observeBetween(from.atDay(1), to.atEndOfMonth())

    suspend fun transaction(id: Long): Transaction? = db.transactionDao().getById(id)

    suspend fun save(transaction: Transaction) = db.transactionDao().upsert(transaction)

    suspend fun delete(transaction: Transaction) = db.transactionDao().delete(transaction)

    fun budgetsIn(month: YearMonth): Flow<List<Budget>> = db.budgetDao().observeForMonth(month)

    suspend fun setBudget(month: YearMonth, categoryId: Long, limit: Long?) {
        if (limit == null || limit <= 0) {
            db.budgetDao().delete(month, categoryId)
        } else {
            db.budgetDao().upsert(Budget(month, categoryId, limit))
        }
    }

    /** Sao chép ngân sách của tháng [from] sang tháng [to]. */
    suspend fun copyBudgets(from: YearMonth, to: YearMonth) {
        db.budgetDao().getForMonth(from).forEach { db.budgetDao().upsert(it.copy(month = to)) }
    }
}
