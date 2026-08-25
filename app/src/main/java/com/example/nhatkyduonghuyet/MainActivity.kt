package com.example.nhatkyduonghuyet

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.nhatkyduonghuyet.ui.navigation.AppNavHost
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import com.example.nhatkyduonghuyet.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var predictor: GlucosePredictor
    @Inject lateinit var scanner: GlucoseScanner

    override fun attachBaseContext(newBase: Context) {
        val helper = LocaleHelper(newBase)
        super.attachBaseContext(helper.applyLocale(newBase))
    }

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
