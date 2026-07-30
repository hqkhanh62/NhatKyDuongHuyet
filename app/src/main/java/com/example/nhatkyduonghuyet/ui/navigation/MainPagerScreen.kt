package com.example.nhatkyduonghuyet.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import com.example.nhatkyduonghuyet.ui.chart.ChartScreen
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardScreenPro
import com.example.nhatkyduonghuyet.ui.home.DateListScreen
import com.example.nhatkyduonghuyet.ui.prediction.PredictionScreen
import com.example.nhatkyduonghuyet.ui.screens.search.SearchScreen
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainPagerScreen(
    navController: androidx.navigation.NavController,
    viewModel: LogEntryViewModel,
    predictor: GlucosePredictor
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Log") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                    label = { Text("Trend") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 3,
                    onClick = { scope.launch { pagerState.animateScrollToPage(3) } },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 4,
                    onClick = { scope.launch { pagerState.animateScrollToPage(4) } },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("AI") }
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> DashboardScreenPro(
                    onViewDetails = { scope.launch { pagerState.animateScrollToPage(1) } },
                    onNavigateToPrediction = { scope.launch { pagerState.animateScrollToPage(4) } }
                )
                1 -> DateListScreen(navController = navController, viewModel = viewModel)
                2 -> ChartScreen(navController = navController, viewModel = viewModel)
                3 -> SearchScreen(navController = navController, viewModel = viewModel)
                4 -> PredictionScreen(navController = navController, predictor = predictor)
            }
        }
    }
}
