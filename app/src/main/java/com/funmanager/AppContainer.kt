package com.funmanager

import android.content.Context
import com.funmanager.data.AppDatabase
import com.funmanager.data.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

/** DI thủ công: giữ các đối tượng dùng chung toàn app. */
class AppContainer(context: Context) {
    val repository = FinanceRepository(AppDatabase.build(context))

    /** Phạm vi coroutine sống cùng app, cho các việc nền ngắn. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Tháng đang xem, dùng chung giữa các tab. */
    val selectedMonth = MutableStateFlow(YearMonth.now())

    /** Tạo các giao dịch định kỳ đã đến hạn. */
    fun runRecurring() {
        appScope.launch { repository.generateDueRecurring() }
    }
}
