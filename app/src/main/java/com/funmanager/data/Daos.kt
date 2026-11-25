package com.funmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, id")
    fun observeAll(): Flow<List<Category>>

    @Upsert
    suspend fun upsert(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Upsert
    suspend fun upsert(transaction: Transaction)

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int

    @Query("UPDATE transactions SET categoryId = :to WHERE categoryId = :from")
    suspend fun moveCategory(from: Long, to: Long)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun observeForMonth(month: YearMonth): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE month = :month")
    suspend fun getForMonth(month: YearMonth): List<Budget>

    @Upsert
    suspend fun upsert(budget: Budget)

    @Query("DELETE FROM budgets WHERE month = :month AND categoryId = :categoryId")
    suspend fun delete(month: YearMonth, categoryId: Long)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteForCategory(categoryId: Long)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules ORDER BY active DESC, dayOfMonth, id")
    fun observeAll(): Flow<List<RecurringRule>>

    @Query("SELECT * FROM recurring_rules WHERE active = 1")
    suspend fun getActive(): List<RecurringRule>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getById(id: Long): RecurringRule?

    @Upsert
    suspend fun upsert(rule: RecurringRule): Long

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE recurring_rules SET categoryId = :to WHERE categoryId = :from")
    suspend fun moveCategory(from: Long, to: Long)

    @Query("SELECT COUNT(*) FROM recurring_rules WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int
}
