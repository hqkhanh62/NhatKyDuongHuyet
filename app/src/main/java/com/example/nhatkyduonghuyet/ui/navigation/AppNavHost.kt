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
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardScreenPro
import com.example.nhatkyduonghuyet.ui.prediction.PredictionScreen
import com.example.nhatkyduonghuyet.ui.screens.search.SearchScreen
import com.example.nhatkyduonghuyet.ui.scanner.ScannerScreen
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel

import androidx.compose.ui.unit.IntOffset

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
            fadeIn(animationSpec = tween<Float>(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween<IntOffset>(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween<Float>(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween<IntOffset>(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween<Float>(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween<IntOffset>(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween<Float>(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween<IntOffset>(300)
            )
        }
    ) {
        composable("main_pager") {
            MainPagerScreen(
                navController = navController,
                viewModel = viewModel,
                predictor = predictor
            )
        }

        composable(GlucoseScreen.DayDetail.route + "/{date}") { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: return@composable
            DayDetailScreen(
                navController = navController,
                viewModel = viewModel,
                selectedDate = date
            )
        }

        composable(GlucoseScreen.Scanner.route) {
            val dashboardViewModel: com.example.nhatkyduonghuyet.ui.dashboard.DashboardViewModel = hiltViewModel()
            ScannerScreen(
                navController = navController,
                scanner = scanner,
                onGlucoseDetected = { result ->
                    dashboardViewModel.onGlucoseScanned(result)
                }
            )
        }
        
        composable(GlucoseScreen.Chart.route) {
            ChartScreen(navController = navController, viewModel = viewModel)
        }
        composable(GlucoseScreen.Search.route) {
            SearchScreen(navController = navController, viewModel = viewModel)
        }
        composable(GlucoseScreen.Prediction.route) {
            PredictionScreen(navController = navController, predictor = predictor)
        }
    }
}
