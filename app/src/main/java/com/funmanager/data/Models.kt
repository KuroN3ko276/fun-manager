package com.funmanager.data

import androidx.room.ColumnInfo
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
