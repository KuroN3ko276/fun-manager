package com.funmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.funmanager.ui.FunManagerNavHost
import com.funmanager.ui.theme.FunManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FunManagerTheme {
                FunManagerNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as FunManagerApp).container.runRecurring()
    }
}
