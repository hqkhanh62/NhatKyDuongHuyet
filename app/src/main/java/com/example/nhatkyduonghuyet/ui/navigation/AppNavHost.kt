package com.example.nhatkyduonghuyet.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.example.nhatkyduonghuyet.ui.settings.SettingsScreen
import com.example.nhatkyduonghuyet.ui.scanner.ScannerScreen
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: LogEntryViewModel,
    predictor: GlucosePredictor,
    scanner: GlucoseScanner,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "main_pager",
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("main_pager") {
            MainPagerScreen(
                navController = navController,
                viewModel = viewModel,
                predictor = predictor
            )
        }

        composable(Screen.Scanner.route) {
            val dashboardViewModel: com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel = hiltViewModel()
            ScannerScreen(
                navController = navController,
                scanner = scanner,
                onGlucoseDetected = { result ->
                    dashboardViewModel.onGlucoseScanned(result)
                }
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

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
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
    object Scanner : Screen("scanner")
    object Settings : Screen("settings")
}
