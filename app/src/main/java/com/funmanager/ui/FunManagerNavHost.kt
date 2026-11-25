package com.funmanager.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.funmanager.R
import com.funmanager.ui.budget.BudgetRoute
import com.funmanager.ui.category.CategoriesRoute
import com.funmanager.ui.edit.EditTransactionRoute
import com.funmanager.ui.edit.EditTransactionViewModel
import com.funmanager.ui.home.HomeRoute
import com.funmanager.ui.recurring.EditRecurringRoute
import com.funmanager.ui.recurring.EditRecurringViewModel
import com.funmanager.ui.recurring.RecurringListRoute
import com.funmanager.ui.report.ReportRoute
import com.funmanager.ui.savings.EditGoalRoute
import com.funmanager.ui.savings.EditGoalViewModel
import com.funmanager.ui.savings.GoalDetailRoute
import com.funmanager.ui.savings.GoalDetailViewModel
import com.funmanager.ui.savings.SavingsRoute
import com.funmanager.ui.search.SearchRoute
import com.funmanager.ui.settings.SecurityRoute
import com.funmanager.ui.wallet.WalletsRoute

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Transactions("home", R.string.tab_transactions, Icons.AutoMirrored.Filled.ReceiptLong),
    Report("report", R.string.tab_report, Icons.Filled.PieChart),
    Budget("budget", R.string.tab_budget, Icons.Filled.AccountBalanceWallet),
    Savings("savings", R.string.tab_savings, Icons.Filled.Savings),
}

private const val EDIT_ROUTE = "edit?${EditTransactionViewModel.ARG_ID}={${EditTransactionViewModel.ARG_ID}}"

private const val CATEGORIES_ROUTE = "categories"
private const val RECURRING_ROUTE = "recurring"
private const val SEARCH_ROUTE = "search"
private const val WALLETS_ROUTE = "wallets"
private const val SECURITY_ROUTE = "security"
private const val RECURRING_EDIT_ROUTE =
    "recurring/edit?${EditRecurringViewModel.ARG_ID}={${EditRecurringViewModel.ARG_ID}}"

private const val GOAL_ROUTE_PREFIX = "savings/goal"
private const val GOAL_DETAIL_ROUTE = "$GOAL_ROUTE_PREFIX/{${GoalDetailViewModel.ARG_ID}}"
private const val GOAL_EDIT_ROUTE = "savings/edit?${EditGoalViewModel.ARG_ID}={${EditGoalViewModel.ARG_ID}}"

private fun goalEditRoute(id: Long = EditGoalViewModel.NEW_ID) = "savings/edit?${EditGoalViewModel.ARG_ID}=$id"

private fun recurringEditRoute(id: Long = EditRecurringViewModel.NEW_ID) =
    "recurring/edit?${EditRecurringViewModel.ARG_ID}=$id"

private fun editRoute(id: Long = EditTransactionViewModel.NEW_ID) = "edit?${EditTransactionViewModel.ARG_ID}=$id"

/** Cỡ chữ nhãn tab: theo cỡ chữ hệ thống nhưng không quá 1.3× để 4 nhãn vẫn nằm trên một dòng. */
@Composable
private fun navLabelSize(): TextUnit {
    val fontScale = LocalDensity.current.fontScale
    val base = MaterialTheme.typography.labelMedium.fontSize
    return if (fontScale > MAX_NAV_LABEL_SCALE) base * (MAX_NAV_LABEL_SCALE / fontScale) else base
}

private const val MAX_NAV_LABEL_SCALE = 1.3f

@Composable
fun FunManagerNavHost(addRequested: Boolean = false, onAddHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    LaunchedEffect(addRequested) {
        if (addRequested) {
            navController.navigate(editRoute()) { launchSingleTop = true }
            onAddHandled()
        }
    }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (Tab.entries.any { it.route == currentRoute }) {
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
                            label = {
                                Text(
                                    stringResource(tab.label),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = navLabelSize(),
                                )
                            },
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
                    onOpenBudget = {
                        navController.navigate(Tab.Budget.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenCategories = { navController.navigate(CATEGORIES_ROUTE) },
                    onOpenRecurring = { navController.navigate(RECURRING_ROUTE) },
                    onOpenSearch = { navController.navigate(SEARCH_ROUTE) },
                    onOpenWallets = { navController.navigate(WALLETS_ROUTE) },
                    onOpenSecurity = { navController.navigate(SECURITY_ROUTE) },
                )
            }
            composable(SECURITY_ROUTE) { SecurityRoute(onBack = { navController.popBackStack() }) }
            composable(WALLETS_ROUTE) { WalletsRoute(onBack = { navController.popBackStack() }) }
            composable(SEARCH_ROUTE) {
                SearchRoute(
                    onBack = { navController.popBackStack() },
                    onOpen = { navController.navigate(editRoute(it)) },
                )
            }
            composable(RECURRING_ROUTE) {
                RecurringListRoute(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(recurringEditRoute()) },
                    onOpen = { navController.navigate(recurringEditRoute(it)) },
                )
            }
            composable(
                RECURRING_EDIT_ROUTE,
                arguments = listOf(
                    navArgument(EditRecurringViewModel.ARG_ID) {
                        type = NavType.LongType
                        defaultValue = EditRecurringViewModel.NEW_ID
                    },
                ),
            ) {
                EditRecurringRoute(onDone = { navController.popBackStack() })
            }
            composable(CATEGORIES_ROUTE) { CategoriesRoute(onBack = { navController.popBackStack() }) }
            composable(Tab.Report.route) { ReportRoute() }
            composable(Tab.Budget.route) { BudgetRoute() }
            composable(Tab.Savings.route) {
                SavingsRoute(
                    onAdd = { navController.navigate(goalEditRoute()) },
                    onOpen = { navController.navigate("$GOAL_ROUTE_PREFIX/$it") },
                )
            }
            composable(
                GOAL_DETAIL_ROUTE,
                arguments = listOf(navArgument(GoalDetailViewModel.ARG_ID) { type = NavType.LongType }),
            ) {
                GoalDetailRoute(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(goalEditRoute(it)) },
                )
            }
            composable(
                GOAL_EDIT_ROUTE,
                arguments = listOf(
                    navArgument(EditGoalViewModel.ARG_ID) {
                        type = NavType.LongType
                        defaultValue = EditGoalViewModel.NEW_ID
                    },
                ),
            ) {
                EditGoalRoute(
                    onSaved = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack(Tab.Savings.route, inclusive = false) },
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
