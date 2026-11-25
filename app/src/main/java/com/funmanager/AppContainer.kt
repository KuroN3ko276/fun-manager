package com.funmanager

import android.content.Context
import com.funmanager.data.AppDatabase
import com.funmanager.data.FinanceRepository
import com.funmanager.data.LockStore
import com.funmanager.data.ThemeStore
import com.funmanager.lock.AppLockManager
import com.funmanager.ui.theme.Accent
import com.funmanager.widget.SummaryWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.YearMonth

/** DI thủ công: giữ các đối tượng dùng chung toàn app. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val repository = FinanceRepository(AppDatabase.build(appContext))

    val appLock = AppLockManager(LockStore(appContext))

    private val themeStore = ThemeStore(appContext)

    /** Màu chủ đạo hiện tại; đổi ở màn Giao diện. */
    val accent = MutableStateFlow(themeStore.accent)

    fun setAccent(value: Accent) {
        themeStore.accent = value
        accent.value = value
        SummaryWidgetProvider.updateAll(appContext)
    }

    /** Phạm vi coroutine sống cùng app, cho các việc nền ngắn. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Tháng đang xem, dùng chung giữa các tab. */
    val selectedMonth = MutableStateFlow(YearMonth.now())

    init {
        watchForWidget()
    }

    /** Tạo các giao dịch định kỳ đã đến hạn. */
    fun runRecurring() {
        appScope.launch { repository.generateDueRecurring() }
    }

    /** Vẽ lại widget mỗi khi giao dịch, ngân sách của tháng hiện tại hoặc danh mục thay đổi. */
    private fun watchForWidget() {
        appScope.launch {
            val month = YearMonth.now()
            combine(repository.transactionsIn(month), repository.budgetsIn(month), repository.categories) { _, _, _ -> }
                .collectLatest {
                    delay(WIDGET_DEBOUNCE_MS)
                    SummaryWidgetProvider.updateAll(appContext)
                }
        }
    }

    private companion object {
        const val WIDGET_DEBOUNCE_MS = 500L
    }
}
