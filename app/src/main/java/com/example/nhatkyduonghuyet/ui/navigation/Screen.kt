package com.example.nhatkyduonghuyet.ui.navigation

sealed class GlucoseScreen(val route: String) {
    object Dashboard : GlucoseScreen("dashboard")
    object DateList : GlucoseScreen("date_list")
    object DayDetail : GlucoseScreen("day_detail")
    object Chart : GlucoseScreen("chart")
    object Search : GlucoseScreen("search")
    object Prediction : GlucoseScreen("prediction")
    object Scanner : GlucoseScreen("scanner")
}
