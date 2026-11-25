package com.funmanager.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Category::class,
        Transaction::class,
        Budget::class,
        RecurringRule::class,
        SavingsGoal::class,
        SavingsDeposit::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun savingsDao(): SavingsDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "fun-manager.db")
                .addCallback(SeedCallback)
                .build()
    }
}

private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultCategories.all.forEach { c ->
            db.execSQL(
                "INSERT INTO categories (name, icon, color, type) VALUES (?, ?, ?, ?)",
                arrayOf(c.name, c.icon, c.color, c.type.name),
            )
        }
    }
}

object DefaultCategories {
    val all = listOf(
        Category(name = "Ăn uống", icon = "food", color = 0xFFEF5350, type = TransactionType.EXPENSE),
        Category(name = "Đi lại", icon = "transport", color = 0xFF42A5F5, type = TransactionType.EXPENSE),
        Category(name = "Nhà ở", icon = "home", color = 0xFF8D6E63, type = TransactionType.EXPENSE),
        Category(name = "Hoá đơn", icon = "bill", color = 0xFFFFA726, type = TransactionType.EXPENSE),
        Category(name = "Mua sắm", icon = "shopping", color = 0xFFAB47BC, type = TransactionType.EXPENSE),
        Category(name = "Giải trí", icon = "entertainment", color = 0xFFEC407A, type = TransactionType.EXPENSE),
        Category(name = "Sức khoẻ", icon = "health", color = 0xFF26A69A, type = TransactionType.EXPENSE),
        Category(name = "Giáo dục", icon = "education", color = 0xFF5C6BC0, type = TransactionType.EXPENSE),
        Category(name = "Khác", icon = "other", color = 0xFF78909C, type = TransactionType.EXPENSE),
        Category(name = "Lương", icon = "salary", color = 0xFF66BB6A, type = TransactionType.INCOME),
        Category(name = "Thưởng", icon = "bonus", color = 0xFF9CCC65, type = TransactionType.INCOME),
        Category(name = "Thu khác", icon = "other", color = 0xFF26C6DA, type = TransactionType.INCOME),
    )
}
