package com.example.nhatkyduonghuyet.util

import android.content.Context
import android.content.SharedPreferences
import java.util.*

class LocaleHelper(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "language"
        const val VIETNAMESE = "vi"
        const val ENGLISH = "en"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setLocale(language: String): Context {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
        return updateResources(language)
    }

    fun getCurrentLocale(): String {
        return prefs.getString(KEY_LANGUAGE, VIETNAMESE) ?: VIETNAMESE
    }

    private fun updateResources(language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun applyLocale(context: Context): Context {
        return updateResources(getCurrentLocale())
    }
}
