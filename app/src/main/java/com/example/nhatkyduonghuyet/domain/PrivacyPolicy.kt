package com.example.nhatkyduonghuyet.domain

import android.content.Context

object PrivacyPolicy {
    private const val PREFS_NAME = "privacy_prefs"
    private const val KEY_HIDE_WIDGET_DATA = "hide_health_data_on_widget"

    fun shouldHideWidgetData(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HIDE_WIDGET_DATA, false)
    }

    fun setHideWidgetData(context: Context, hide: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HIDE_WIDGET_DATA, hide)
            .apply()
    }
}
