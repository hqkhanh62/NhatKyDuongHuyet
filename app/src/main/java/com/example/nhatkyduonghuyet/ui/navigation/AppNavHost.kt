package com.example.nhatkyduonghuyet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import com.example.nhatkyduonghuyet.ui.home.DateListScreen
import com.example.nhatkyduonghuyet.ui.detail.DayDetailScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: LogEntryViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = Screen.DateList.route, modifier = modifier) {
        composable(Screen.DateList.route) {
            DateListScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.DayDetail.route + "/{date}") {
            val date = it.arguments?.getString("date") ?: return@composable
            DayDetailScreen(navController = navController, viewModel = viewModel, selectedDate = date)
        }
        composable(Screen.Chart.route + "/{chartType}") {
            val chartType = it.arguments?.getString("chartType") ?: return@composable
            ChartScreen(navController = navController, viewModel = viewModel, chartType = chartType)
        }
    }
}

sealed class Screen(val route: String) {
    object DateList : Screen("date_list")
    object DayDetail : Screen("day_detail")
    object Chart : Screen("chart")
}
