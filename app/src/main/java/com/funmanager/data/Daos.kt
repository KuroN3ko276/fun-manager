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

    @Query("SELECT * FROM categories ORDER BY type, id")
    suspend fun getAll(): List<Category>

    @Upsert
    suspend fun upsert(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Upsert
    suspend fun upsert(transaction: Transaction)

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = :type AND date BETWEEN :start AND :end")
    suspend fun sumBetween(type: TransactionType, start: LocalDate, end: LocalDate): Long

    @Query(
        """
        SELECT categoryId, SUM(amount) AS total FROM transactions
        WHERE type = 'EXPENSE' AND date BETWEEN :start AND :end GROUP BY categoryId
        """,
    )
    suspend fun expenseByCategory(start: LocalDate, end: LocalDate): List<CategorySum>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int

    @Query("UPDATE transactions SET categoryId = :to WHERE categoryId = :from")
    suspend fun moveCategory(from: Long, to: Long)
}

@Dao
interface BudgetDao {
    /** Mọi lần đặt hạn mức từ trước tới [month] (tháng lưu dạng "yyyy-MM" nên so sánh chuỗi đúng thứ tự). */
    @Query("SELECT * FROM budgets WHERE month <= :month")
    fun observeUpTo(month: YearMonth): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE month <= :month")
    suspend fun getUpTo(month: YearMonth): List<Budget>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId")
    suspend fun getForCategory(categoryId: Long): List<Budget>

    @Upsert
    suspend fun upsert(budget: Budget)

    @Query("DELETE FROM budgets WHERE month = :month AND categoryId = :categoryId")
    suspend fun delete(month: YearMonth, categoryId: Long)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteForCategory(categoryId: Long)

    @Query("SELECT * FROM budget_adjustments WHERE month = :month ORDER BY id")
    fun observeAdjustments(month: YearMonth): Flow<List<BudgetAdjustment>>

    @Query("SELECT * FROM budget_adjustments WHERE month = :month ORDER BY id")
    suspend fun getAdjustments(month: YearMonth): List<BudgetAdjustment>

    @Insert
    suspend fun insertAdjustment(adjustment: BudgetAdjustment)

    @Query("DELETE FROM budget_adjustments WHERE id = :id")
    suspend fun deleteAdjustment(id: Long)

    @Query("DELETE FROM budget_adjustments WHERE fromCategoryId = :categoryId OR toCategoryId = :categoryId")
    suspend fun deleteAdjustmentsForCategory(categoryId: Long)
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

@Dao
interface SavingsDao {
    @Query(
        """
        SELECT g.*, COALESCE((SELECT SUM(d.amount) FROM savings_deposits d WHERE d.goalId = g.id), 0) AS saved
        FROM savings_goals g ORDER BY g.id
        """,
    )
    fun observeGoals(): Flow<List<GoalWithSaved>>

    @Query(
        """
        SELECT g.*, COALESCE((SELECT SUM(d.amount) FROM savings_deposits d WHERE d.goalId = g.id), 0) AS saved
        FROM savings_goals g WHERE g.id = :id
        """,
    )
    fun observeGoal(id: Long): Flow<GoalWithSaved?>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoal(id: Long): SavingsGoal?

    @Upsert
    suspend fun upsertGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM savings_deposits WHERE goalId = :goalId ORDER BY date DESC, id DESC")
    fun observeDeposits(goalId: Long): Flow<List<SavingsDeposit>>

    @Insert
    suspend fun insertDeposit(deposit: SavingsDeposit)

    @Query("DELETE FROM savings_deposits WHERE id = :id")
    suspend fun deleteDeposit(id: Long)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY id")
    fun observeAll(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets ORDER BY id")
    suspend fun getAll(): List<Wallet>

    @Upsert
    suspend fun upsert(wallet: Wallet)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun delete(id: Long)

    /** Thu − chi của từng ví: [WalletNet.net] tính tới hết ngày [today], [WalletNet.upcoming] là các ngày sau đó. */
    @Query(
        """
        SELECT walletId,
            SUM(CASE WHEN date <= :today THEN (CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) ELSE 0 END) AS net,
            SUM(CASE WHEN date > :today THEN (CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) ELSE 0 END) AS upcoming
        FROM transactions GROUP BY walletId
        """,
    )
    fun observeNets(today: LocalDate): Flow<List<WalletNet>>

    @Query("SELECT * FROM transfers ORDER BY date DESC, id DESC")
    fun observeTransfers(): Flow<List<Transfer>>

    @Insert
    suspend fun insertTransfer(transfer: Transfer)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteTransfer(id: Long)

    @Query(
        """
        SELECT (SELECT COUNT(*) FROM transactions WHERE walletId = :id)
             + (SELECT COUNT(*) FROM recurring_rules WHERE walletId = :id)
             + (SELECT COUNT(*) FROM transfers WHERE fromWalletId = :id OR toWalletId = :id)
        """,
    )
    suspend fun usage(id: Long): Int

    @Query("UPDATE transactions SET walletId = :to WHERE walletId = :from")
    suspend fun moveTransactions(from: Long, to: Long)

    @Query("UPDATE recurring_rules SET walletId = :to WHERE walletId = :from")
    suspend fun moveRecurring(from: Long, to: Long)

    @Query("UPDATE transfers SET fromWalletId = :to WHERE fromWalletId = :from")
    suspend fun moveTransfersFrom(from: Long, to: Long)

    @Query("UPDATE transfers SET toWalletId = :to WHERE toWalletId = :from")
    suspend fun moveTransfersTo(from: Long, to: Long)

    @Query("DELETE FROM transfers WHERE fromWalletId = toWalletId")
    suspend fun deleteSelfTransfers()

    @Query("UPDATE wallets SET initialBalance = initialBalance + :amount WHERE id = :id")
    suspend fun addToInitialBalance(id: Long, amount: Long)
}
