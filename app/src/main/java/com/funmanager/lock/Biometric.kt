package com.funmanager.lock

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import com.funmanager.R

/** Vân tay / khuôn mặt dùng API hệ thống (Android 11+), không cần thư viện ngoài. */
object Biometric {
    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val manager = context.getSystemService(BiometricManager::class.java) ?: return false
        return try {
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: SecurityException) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun authenticate(activity: Activity, onSuccess: () -> Unit, onDone: () -> Unit) {
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.biometric_title))
            .setSubtitle(activity.getString(R.string.app_name))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButton(activity.getString(R.string.use_pin), activity.mainExecutor) { _, _ -> onDone() }
            .build()
        prompt.authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                    onDone()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onDone()
            },
        )
    }
}
