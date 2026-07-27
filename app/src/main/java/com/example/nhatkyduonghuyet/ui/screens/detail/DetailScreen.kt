package com.example.nhatkyduonghuyet.ui.screens.detail

import androidx.navigation.NavController
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nhatkyduonghuyet.viewmodel.DetailViewModel
import com.example.nhatkyduonghuyet.ui.components.SessionCardV2
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DayDetailScreen(
    navController: NavController,
    selectedDate: String,
    viewModel: DetailViewModel = hiltViewModel() // Đã sửa LogEntryViewModel thành DetailViewModel tại đây
) {
}