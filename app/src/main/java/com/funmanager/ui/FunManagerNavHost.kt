package com.funmanager.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.funmanager.R
import com.funmanager.ui.edit.EditTransactionRoute
import com.funmanager.ui.edit.EditTransactionViewModel
import com.funmanager.ui.home.HomeRoute

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Transactions("home", R.string.tab_transactions, Icons.AutoMirrored.Filled.ReceiptLong),
}

private const val EDIT_ROUTE = "edit?${EditTransactionViewModel.ARG_ID}={${EditTransactionViewModel.ARG_ID}}"

private fun editRoute(id: Long = EditTransactionViewModel.NEW_ID) = "edit?${EditTransactionViewModel.ARG_ID}=$id"

@Composable
fun FunManagerNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (Tab.entries.size > 1 && Tab.entries.any { it.route == currentRoute }) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Tab.Transactions.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Transactions.route) {
                HomeRoute(
                    onAdd = { navController.navigate(editRoute()) },
                    onOpen = { navController.navigate(editRoute(it)) },
                )
            }
            composable(
                EDIT_ROUTE,
                arguments = listOf(
                    navArgument(EditTransactionViewModel.ARG_ID) {
                        type = NavType.LongType
                        defaultValue = EditTransactionViewModel.NEW_ID
                    },
                ),
            ) {
                EditTransactionRoute(onDone = { navController.popBackStack() })
            }
        }
    }
}
