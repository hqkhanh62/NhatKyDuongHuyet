package com.example.nhatkyduonghuyet.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import com.example.nhatkyduonghuyet.ui.chart.ChartScreen
import com.example.nhatkyduonghuyet.ui.dashboard.DashboardScreenPro
import com.example.nhatkyduonghuyet.ui.home.DateListScreen
import com.example.nhatkyduonghuyet.ui.prediction.PredictionScreen
import com.example.nhatkyduonghuyet.ui.screens.search.SearchScreen
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

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
            NavigationBar(
                tonalElevation = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val items = listOf(
                    Triple(0, Icons.Default.Home, "Home"),
                    Triple(1, Icons.Default.List, "Log"),
                    Triple(2, Icons.Default.TrendingUp, "Trend"),
                    Triple(3, Icons.Default.Search, "Search"),
                    Triple(4, Icons.Default.AutoAwesome, "AI")
                )

                items.forEach { (index, icon, label) ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondBoundsPageCount = 1
        ) { page ->
            // Custom Animation Logic: Scale and Alpha based on distance from current page
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Fade effect: reduce alpha as page moves away
                        alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        // Scale effect: slightly shrink as page moves away
                        val scale = lerp(
                            start = 0.85f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                when (page) {
                    0 -> DashboardScreenPro(
                        onViewDetails = { scope.launch { pagerState.animateScrollToPage(1) } },
                        onNavigateToPrediction = { scope.launch { pagerState.animateScrollToPage(4) } },
                        onNavigateToScanner = { navController.navigate(GlucoseScreen.Scanner.route) },
                        onNavigateToMedication = { navController.navigate(GlucoseScreen.Medication.route) }
                    )
                    1 -> DateListScreen(navController = navController, viewModel = viewModel)
                    2 -> ChartScreen(navController = navController, viewModel = viewModel)
                    3 -> SearchScreen(navController = navController, viewModel = viewModel)
                    4 -> PredictionScreen(navController = navController, predictor = predictor)
                }
            }
        }
    }
}
