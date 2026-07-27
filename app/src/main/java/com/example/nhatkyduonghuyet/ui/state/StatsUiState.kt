package com.example.nhatkyduonghuyet.ui.state

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(
        val avg: Double,
        val min: Double,
        val max: Double
    ) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}
