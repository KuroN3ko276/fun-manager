package com.funmanager.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.YearMonth

enum class TransactionType { INCOME, EXPENSE }

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Khoá biểu tượng, ánh xạ sang ImageVector ở tầng UI. */
    val icon: String,
    /** Màu ARGB. */
    val color: Long,
    val type: TransactionType,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId"), Index("date")],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Số tiền VNĐ, luôn dương; chiều thu/chi do [type] quyết định. */
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long,
    val date: LocalDate,
    val note: String = "",
    /** Id của [RecurringRule] đã tạo ra giao dịch này, null nếu nhập tay. */
    @ColumnInfo(defaultValue = "NULL") val recurringId: Long? = null,
    @ColumnInfo(defaultValue = Wallet.DEFAULT_ID_SQL, index = true) val walletId: Long = Wallet.DEFAULT_ID,
)

@Entity(tableName = "recurring_rules", indices = [Index("categoryId")])
data class RecurringRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long,
    /** Ngày trong tháng (1..31); tháng ngắn hơn sẽ dùng ngày cuối tháng. */
    val dayOfMonth: Int,
    val note: String = "",
    /** Tháng kế tiếp cần tạo giao dịch. */
    val nextMonth: YearMonth,
    val active: Boolean = true,
    @ColumnInfo(defaultValue = Wallet.DEFAULT_ID_SQL) val walletId: Long = Wallet.DEFAULT_ID,
)

@Entity(tableName = "budgets", primaryKeys = ["month", "categoryId"])
data class Budget(
    val month: YearMonth,
    /** [TOTAL_CATEGORY_ID] nghĩa là ngân sách tổng của tháng. */
    val categoryId: Long,
    val limit: Long,
) {
    companion object {
        const val TOTAL_CATEGORY_ID = 0L
    }
}

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Số tiền mục tiêu (VNĐ). */
    val target: Long,
    val icon: String,
    val color: Long,
    /** Hạn hoàn thành, null nếu không đặt hạn. */
    val deadline: LocalDate? = null,
)

@Entity(
    tableName = "savings_deposits",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("goalId")],
)
data class SavingsDeposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    /** Dương = nạp vào, âm = rút ra. */
    val amount: Long,
    val date: LocalDate,
    val note: String = "",
)

/** Mục tiêu kèm tổng số đã để dành. */
data class GoalWithSaved(
    @Embedded val goal: SavingsGoal,
    val saved: Long,
)

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    /** Số dư khi bắt đầu dùng app (có thể âm, ví dụ thẻ tín dụng). */
    val initialBalance: Long = 0,
) {
    companion object {
        /** Ví "Tiền mặt" tạo sẵn; giao dịch cũ được gán vào ví này khi nâng cấp. */
        const val DEFAULT_ID = 1L
        const val DEFAULT_ID_SQL = "1"
    }
}

/** Chuyển tiền giữa hai ví: không tính là thu hay chi. */
@Entity(tableName = "transfers", indices = [Index("fromWalletId"), Index("toWalletId")])
data class Transfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromWalletId: Long,
    val toWalletId: Long,
    val amount: Long,
    val date: LocalDate,
    val note: String = "",
)

/** Tổng thu − chi của một ví. */
data class WalletNet(val walletId: Long, val net: Long)

/** Tổng tiền theo danh mục (kết quả truy vấn gộp). */
data class CategorySum(val categoryId: Long, val total: Long)
