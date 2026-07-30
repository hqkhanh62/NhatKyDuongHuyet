package com.example.nhatkyduonghuyet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.nhatkyduonghuyet.ui.chart.ChartScreen
import com.example.nhatkyduonghuyet.ui.detail.DayDetailScreen
import com.example.nhatkyduonghuyet.ui.home.DateListScreen
import com.example.nhatkyduonghuyet.ui.screens.dashboard.DashboardScreen
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardScreenPro
import com.example.nhatkyduonghuyet.ui.navigation.MainPagerScreen
import com.example.nhatkyduonghuyet.ui.prediction.PredictionScreen
import com.example.nhatkyduonghuyet.ui.screens.search.SearchScreen
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: LogEntryViewModel,
    predictor: GlucosePredictor,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "main_pager",
        modifier = modifier
    ) {
        composable("main_pager") {
            MainPagerScreen(
                navController = navController,
                viewModel = viewModel,
                predictor = predictor
            )
        }

        composable(Screen.DayDetail.route + "/{date}") { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: return@composable
            DayDetailScreen(
                navController = navController,
                viewModel = viewModel,
                selectedDate = date
            )
        }

        composable(Screen.Chart.route) {
            ChartScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(Screen.Prediction.route) {
            PredictionScreen(
                navController = navController,
                predictor = predictor
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object DateList : Screen("date_list")
    object DayDetail : Screen("day_detail")
    object Chart : Screen("chart")
    object Search : Screen("search")
    object Prediction : Screen("prediction")
}
