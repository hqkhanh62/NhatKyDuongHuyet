package com.example.nhatkyduonghuyet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.nhatkyduonghuyet.ui.navigation.AppNavHost
import com.example.nhatkyduonghuyet.ui.theme.NhatKyDuongHuyetTheme
import com.example.nhatkyduonghuyet.viewmodel.LogEntryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NhatKyDuongHuyetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val application = application as NhatKyDuongHuyetApplication
                    val viewModel: LogEntryViewModel = viewModel(factory = LogEntryViewModel.provideFactory(application.container.logEntryRepository))
                    AppNavHost(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NhatKyDuongHuyetTheme {
        // Preview content here if needed
    }
}
