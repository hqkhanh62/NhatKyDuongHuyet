package com.example.nhatkyduonghuyet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.nhatkyduonghuyet.ui.navigation.AppNavHost
import com.example.nhatkyduonghuyet.ui.navigation.GlucoseScreen
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel
import com.example.nhatkyduonghuyet.ml.GlucosePredictor
import com.example.nhatkyduonghuyet.ml.GlucoseScanner
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var predictor: GlucosePredictor
    @Inject lateinit var scanner: GlucoseScanner
    
    private var pendingDeepLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        
        setContent {
            val navController = rememberNavController()
            val viewModel: LogEntryViewModel = hiltViewModel()

            LaunchedEffect(pendingDeepLink) {
                pendingDeepLink?.let {
                    navController.navigate(it)
                    pendingDeepLink = null
                }
            }

            AppNavHost(
                navController = navController,
                viewModel = viewModel,
                predictor = predictor,
                scanner = scanner
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Important: update the intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.example.nhatkyduonghuyet.OPEN_DAY_DETAIL") {
            val date = intent.getStringExtra("date") ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            pendingDeepLink = "${GlucoseScreen.DayDetail.route}/$date"
        }
    }
}
