package com.funmanager.data

import androidx.room.withTransaction
import com.funmanager.domain.MonthSnapshot
import com.funmanager.domain.dueOccurrences
import com.funmanager.domain.resumeMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth

class FinanceRepository(private val db: AppDatabase) {
    private val recurringMutex = Mutex()

    val categories: Flow<List<Category>> = db.categoryDao().observeAll()

    suspend fun saveCategory(category: Category) = db.categoryDao().upsert(category)

    /** Số giao dịch và quy tắc định kỳ đang dùng danh mục. */
    suspend fun categoryUsage(categoryId: Long): Int =
        db.transactionDao().countForCategory(categoryId) + db.recurringDao().countForCategory(categoryId)

    /** Xoá danh mục; giao dịch và quy tắc định kỳ đang dùng nó được chuyển sang [replacementId], ngân sách của nó bị xoá. */
    suspend fun deleteCategory(categoryId: Long, replacementId: Long?) = db.withTransaction {
        if (replacementId != null) {
            db.transactionDao().moveCategory(categoryId, replacementId)
            db.recurringDao().moveCategory(categoryId, replacementId)
        }
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

    val recurringRules: Flow<List<RecurringRule>> = db.recurringDao().observeAll()

    suspend fun recurringRule(id: Long): RecurringRule? = db.recurringDao().getById(id)

    /** Lưu quy tắc; nếu lần phát sinh đã đến hạn (ví dụ đúng hôm nay) thì tạo luôn. */
    suspend fun saveRecurring(rule: RecurringRule, today: LocalDate = LocalDate.now()) {
        db.recurringDao().upsert(rule.copy(nextMonth = resumeMonth(rule.nextMonth, rule.dayOfMonth, today)))
        generateDueRecurring(today)
    }

    suspend fun deleteRecurring(id: Long) = db.recurringDao().delete(id)

    /** Tạo các giao dịch định kỳ đã đến hạn; trả về số giao dịch đã tạo. */
    suspend fun generateDueRecurring(today: LocalDate = LocalDate.now()): Int = recurringMutex.withLock {
        db.withTransaction {
            var created = 0
            db.recurringDao().getActive().forEach { rule ->
                val due = dueOccurrences(rule.dayOfMonth, rule.nextMonth, today)
                if (due.dates.isEmpty()) return@forEach
                db.transactionDao().insertAll(
                    due.dates.map { date ->
                        Transaction(
                            amount = rule.amount,
                            type = rule.type,
                            categoryId = rule.categoryId,
                            date = date,
                            note = rule.note,
                            recurringId = rule.id,
                        )
                    },
                )
                db.recurringDao().upsert(rule.copy(nextMonth = due.nextMonth))
                created += due.dates.size
            }
            created
        }
    }

    val savingsGoals: Flow<List<GoalWithSaved>> = db.savingsDao().observeGoals()

    fun savingsGoal(id: Long): Flow<GoalWithSaved?> = db.savingsDao().observeGoal(id)

    suspend fun getSavingsGoal(id: Long): SavingsGoal? = db.savingsDao().getGoal(id)

    suspend fun saveSavingsGoal(goal: SavingsGoal) = db.savingsDao().upsertGoal(goal)

    /** Xoá mục tiêu cùng toàn bộ lịch sử nạp/rút. */
    suspend fun deleteSavingsGoal(id: Long) = db.savingsDao().deleteGoal(id)

    fun savingsDeposits(goalId: Long): Flow<List<SavingsDeposit>> = db.savingsDao().observeDeposits(goalId)

    suspend fun addSavingsDeposit(deposit: SavingsDeposit) = db.savingsDao().insertDeposit(deposit)

    suspend fun deleteSavingsDeposit(id: Long) = db.savingsDao().deleteDeposit(id)

    /** Số liệu tóm tắt của [month] cho widget. */
    suspend fun monthSnapshot(month: YearMonth): MonthSnapshot {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        return MonthSnapshot(
            month = month,
            income = db.transactionDao().sumBetween(TransactionType.INCOME, start, end),
            expense = db.transactionDao().sumBetween(TransactionType.EXPENSE, start, end),
            totalBudget = db.budgetDao().getForMonth(month).firstOrNull { it.categoryId == Budget.TOTAL_CATEGORY_ID }?.limit,
        )
    }
}
