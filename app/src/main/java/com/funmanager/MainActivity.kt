package com.funmanager

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funmanager.lock.AppLockManager
import com.funmanager.lock.Biometric
import com.funmanager.ui.FunManagerNavHost
import com.funmanager.ui.components.AppBackground
import com.funmanager.ui.lock.LockScreen
import com.funmanager.ui.theme.FunManagerTheme
import java.time.YearMonth

class MainActivity : ComponentActivity() {

    /** true khi được mở từ nút ＋ của widget; NavHost sẽ chuyển tới màn thêm giao dịch rồi đặt lại. */
    private val addRequested = mutableStateOf(false)

    private val container get() = (application as FunManagerApp).container
    private val lock: AppLockManager get() = container.appLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val locked by lock.locked.collectAsStateWithLifecycle()
            val settings by lock.settings.collectAsStateWithLifecycle()
            val accent by container.accent.collectAsStateWithLifecycle()
            LaunchedEffect(settings.enabled) {
                // Ẩn nội dung app trong màn hình đa nhiệm khi bật khoá.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(!settings.enabled)
            }
            FunManagerTheme(accent) {
                AppBackground {
                    FunManagerNavHost(
                        addRequested = addRequested.value,
                        onAddHandled = { addRequested.value = false },
                    )
                    if (locked) {
                        LockScreen(
                            pinLength = settings.pinLength,
                            initialLockoutMs = lock.lockoutRemainingMs(),
                            onSubmit = lock::unlockWithPin,
                            onBiometric = if (settings.biometric && Biometric.isAvailable(this@MainActivity)) {
                                ::promptBiometric
                            } else {
                                null
                            },
                            onExit = { moveTaskToBack(true) },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lock.onAppStarted()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) lock.onAppStopped()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        container.runRecurring()
    }

    private fun promptBiometric() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || lock.authenticating) return
        lock.authenticating = true
        Biometric.authenticate(
            activity = this,
            onSuccess = lock::unlockWithBiometric,
            onDone = { lock.authenticating = false },
        )
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ADD_TRANSACTION, false) == true) {
            // Thêm nhanh từ widget luôn ghi vào tháng hiện tại.
            container.selectedMonth.value = YearMonth.now()
            addRequested.value = true
        }
    }

    companion object {
        const val EXTRA_ADD_TRANSACTION = "com.funmanager.extra.ADD_TRANSACTION"
    }
}
