package com.example.nhatkyduonghuyet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.nhatkyduonghuyet.ui.navigation.AppNavHost
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var predictor: GlucosePredictor
    @Inject lateinit var scanner: GlucoseScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                viewModel = hiltViewModel(),
                predictor = predictor,
                scanner = scanner
            )
        }
    }
}
