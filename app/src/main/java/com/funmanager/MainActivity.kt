package com.funmanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.funmanager.ui.FunManagerNavHost
import com.funmanager.ui.theme.FunManagerTheme
import java.time.YearMonth

class MainActivity : ComponentActivity() {

    /** true khi được mở từ nút ＋ của widget; NavHost sẽ chuyển tới màn thêm giao dịch rồi đặt lại. */
    private val addRequested = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            FunManagerTheme {
                FunManagerNavHost(
                    addRequested = addRequested.value,
                    onAddHandled = { addRequested.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        (application as FunManagerApp).container.runRecurring()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ADD_TRANSACTION, false) == true) {
            // Thêm nhanh từ widget luôn ghi vào tháng hiện tại.
            (application as FunManagerApp).container.selectedMonth.value = YearMonth.now()
            addRequested.value = true
        }
    }

    companion object {
        const val EXTRA_ADD_TRANSACTION = "com.funmanager.extra.ADD_TRANSACTION"
    }
}
