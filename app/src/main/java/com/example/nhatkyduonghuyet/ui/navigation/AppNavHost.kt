package com.example.nhatkyduonghuyet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nhatkyduonghuyet.ui.screens.dashboard.DashboardScreen
import com.example.nhatkyduonghuyet.ui.screens.detail.DetailScreen
import com.example.nhatkyduonghuyet.ui.screens.stats.StatsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Detail : Screen("detail/{date}")
    object Stats : Screen("stats")
    object Scanner : Screen("scanner")
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onViewDetails = {
                    navController.navigate(Screen.Stats.route)
                }
            )
        }

        composable(Screen.Detail.route) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            DetailScreen(selectedDate = date)
        }

        composable(Screen.Stats.route) {
            StatsScreen()
        }
    }
}
