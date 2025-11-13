package com.helaluddin.noteghor.data.utils

import android.annotation.SuppressLint
import android.content.Context

object ThemeHelper {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    const val SUNSET = "Sunset"
    const val OCEAN = "Ocean"
    const val FOREST = "Forest"
    const val MIDNIGHT = "Midnight"
    const val CORAL = "Coral"

    fun applyTheme(theme: String, context: Context) {
        saveTheme(theme, context)
    }

    @SuppressLint("UseKtx")
    private fun saveTheme(theme: String, context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme).apply()
    }

    fun getSavedTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, SUNSET) ?: SUNSET
    }
}