package com.funmanager.data

import androidx.room.withTransaction
import com.funmanager.domain.BudgetChange
import com.funmanager.domain.BudgetProgress
import com.funmanager.domain.DailyAllowance
import com.funmanager.domain.MonthSnapshot
import com.funmanager.domain.ReallocationError
import com.funmanager.domain.applyAdjustments
import com.funmanager.domain.budgetChange
import com.funmanager.domain.dailyAllowance
import com.funmanager.domain.effectiveBudgets
import com.funmanager.domain.maxReallocatable
import com.funmanager.domain.validateReallocation
import com.funmanager.domain.WidgetCategoryLine
import com.funmanager.domain.widgetCategoryLines
import com.funmanager.domain.dueOccurrences
import com.funmanager.domain.resumeMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
        db.budgetDao().deleteAdjustmentsForCategory(categoryId)
        db.categoryDao().delete(categoryId)
    }

    fun transactionsIn(month: YearMonth): Flow<List<Transaction>> =
        db.transactionDao().observeBetween(month.atDay(1), month.atEndOfMonth())

    /** Giao dịch từ đầu tháng [from] đến hết tháng [to]. */
    fun transactionsBetween(from: YearMonth, to: YearMonth): Flow<List<Transaction>> =
        db.transactionDao().observeBetween(from.atDay(1), to.atEndOfMonth())

    val allTransactions: Flow<List<Transaction>> = db.transactionDao().observeAll()

    suspend fun transaction(id: Long): Transaction? = db.transactionDao().getById(id)

    suspend fun save(transaction: Transaction) = db.transactionDao().upsert(transaction)

    suspend fun delete(transaction: Transaction) = db.transactionDao().delete(transaction)

    /** Hạn mức gốc của [month] (kế thừa từ lần đặt gần nhất trước đó), chưa tính các lần chuyển trong tháng. */
    fun baseBudgetsIn(month: YearMonth): Flow<List<Budget>> =
        db.budgetDao().observeUpTo(month).map { effectiveBudgets(it, month) }

    /** Hạn mức đang áp dụng cho [month], đã tính các lần chuyển hạn mức trong tháng. */
    fun budgetsIn(month: YearMonth): Flow<List<Budget>> =
        combine(baseBudgetsIn(month), adjustmentsIn(month)) { base, adj -> applyAdjustments(base, adj, month) }

    fun adjustmentsIn(month: YearMonth): Flow<List<BudgetAdjustment>> = db.budgetDao().observeAdjustments(month)

    private suspend fun effectiveBudgetsAt(month: YearMonth): List<Budget> =
        applyAdjustments(effectiveBudgets(db.budgetDao().getUpTo(month), month), db.budgetDao().getAdjustments(month), month)

    /**
     * Chuyển [amount] từ hạn mức [fromId] sang [toId], chỉ trong [month]. Kiểm tra lại số tối đa
     * (phần chưa tiêu của [fromId]) ngay trong transaction; trả về lỗi nếu không hợp lệ.
     */
    suspend fun reallocate(month: YearMonth, fromId: Long, toId: Long, amount: Long): ReallocationError? =
        db.withTransaction {
            val limit = effectiveBudgetsAt(month).firstOrNull { it.categoryId == fromId }?.limit
            val spent = db.transactionDao().expenseByCategory(month.atDay(1), month.atEndOfMonth())
                .firstOrNull { it.categoryId == fromId }?.total ?: 0L
            val max = maxReallocatable(limit?.let { BudgetProgress(spent, it) })
            validateReallocation(fromId, toId, amount, max).also { error ->
                if (error == null) db.budgetDao().insertAdjustment(BudgetAdjustment(month = month, fromCategoryId = fromId, toCategoryId = toId, amount = amount))
            }
        }

    suspend fun deleteAdjustment(id: Long) = db.budgetDao().deleteAdjustment(id)

    /** Đặt (hoặc bỏ khi [limit] null/≤0) hạn mức từ [month] trở đi, cho tới lần sửa tiếp theo. */
    suspend fun setBudget(month: YearMonth, categoryId: Long, limit: Long?) = db.withTransaction {
        when (val change = budgetChange(db.budgetDao().getForCategory(categoryId), month, categoryId, limit)) {
            is BudgetChange.Upsert -> db.budgetDao().upsert(change.budget)
            is BudgetChange.Delete -> db.budgetDao().delete(change.month, change.categoryId)
        }
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
                            walletId = rule.walletId,
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
            totalBudget = effectiveBudgetsAt(month).firstOrNull { it.categoryId == Budget.TOTAL_CATEGORY_ID }?.limit,
        )
    }

    val wallets: Flow<List<Wallet>> = db.walletDao().observeAll()

    fun walletNets(today: LocalDate): Flow<List<WalletNet>> = db.walletDao().observeNets(today)

    val transfers: Flow<List<Transfer>> = db.walletDao().observeTransfers()

    suspend fun saveWallet(wallet: Wallet) = db.walletDao().upsert(wallet)

    suspend fun walletUsage(id: Long): Int = db.walletDao().usage(id)

    /**
     * Xoá ví bằng cách gộp vào [replacementId]: giao dịch, định kỳ, chuyển tiền và số dư ban đầu
     * chuyển sang ví thay thế nên tổng tài sản không đổi.
     */
    suspend fun deleteWallet(wallet: Wallet, replacementId: Long) = db.withTransaction {
        val dao = db.walletDao()
        dao.moveTransactions(wallet.id, replacementId)
        dao.moveRecurring(wallet.id, replacementId)
        dao.moveTransfersFrom(wallet.id, replacementId)
        dao.moveTransfersTo(wallet.id, replacementId)
        dao.deleteSelfTransfers()
        dao.addToInitialBalance(replacementId, wallet.initialBalance)
        dao.delete(wallet.id)
    }

    suspend fun addTransfer(transfer: Transfer) = db.walletDao().insertTransfer(transfer)

    suspend fun deleteTransfer(id: Long) = db.walletDao().deleteTransfer(id)

    /** Số được tiêu mỗi ngày của tháng chứa [today] (null nếu chưa đặt ngân sách); dùng cho widget và nhắc nhở. */
    suspend fun dailyAllowance(today: LocalDate = LocalDate.now()): DailyAllowance? {
        val month = YearMonth.from(today)
        return dailyAllowance(
            today = today,
            budgets = effectiveBudgetsAt(month),
            transactions = db.transactionDao().getBetween(month.atDay(1), month.atEndOfMonth()),
            recurring = db.recurringDao().getActive(),
        )
    }

    /** Dữ liệu widget: tổng tháng + các dòng danh mục đã chọn (null = mặc định). */
    suspend fun widgetData(month: YearMonth, selectedIds: List<Long>?): WidgetData {
        val budgets = effectiveBudgetsAt(month)
        val spent = db.transactionDao().expenseByCategory(month.atDay(1), month.atEndOfMonth())
            .associate { it.categoryId to it.total }
        return WidgetData(
            snapshot = monthSnapshot(month),
            lines = widgetCategoryLines(selectedIds, db.categoryDao().getAll(), spent, budgets),
            allowance = if (month == YearMonth.now()) dailyAllowance() else null,
        )
    }

    suspend fun allCategories(): List<Category> = db.categoryDao().getAll()
}

data class WidgetData(val snapshot: MonthSnapshot, val lines: List<WidgetCategoryLine>, val allowance: DailyAllowance?)
