package com.example.expense_tracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*

/* ── Bottom‑nav destinations ────────────────────────────────── */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Expenses : Screen("expenses", "Expenses", Icons.Default.Receipt)
    object Page2    : Screen("page2",    "Page 2",  Icons.Default.BarChart)
    object Page3    : Screen("page3",    "Page 3",  Icons.Default.Settings)
    companion object { val items = listOf(Expenses, Page2, Page3) }
}

/* ── Root scaffold with bottom navigation ───────────────────── */
@Composable
fun ExpenseTrackerApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Screen.items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState   = true
                            }
                        },
                        icon  = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Expenses.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Expenses.route) { ExpensesScreen() }
            composable(Screen.Page2.route)    { Page2Screen() }
            composable(Screen.Page3.route)    { Page3Screen() }
        }
    }
}
