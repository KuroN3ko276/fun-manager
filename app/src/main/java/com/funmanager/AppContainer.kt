package com.funmanager

import android.content.Context
import com.funmanager.data.AppDatabase
import com.funmanager.data.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.YearMonth

/** DI thủ công: giữ các đối tượng dùng chung toàn app. */
class AppContainer(context: Context) {
    val repository = FinanceRepository(AppDatabase.build(context))

    /** Tháng đang xem, dùng chung giữa các tab. */
    val selectedMonth = MutableStateFlow(YearMonth.now())
}
