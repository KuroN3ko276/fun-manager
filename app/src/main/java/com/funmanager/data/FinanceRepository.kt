package com.funmanager.data

import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

class FinanceRepository(private val db: AppDatabase) {
    val categories: Flow<List<Category>> = db.categoryDao().observeAll()

    fun transactionsIn(month: YearMonth): Flow<List<Transaction>> =
        db.transactionDao().observeBetween(month.atDay(1), month.atEndOfMonth())

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
}
