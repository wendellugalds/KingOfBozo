package com.wendellugalds.kingofbozo.util

import android.content.Context
import android.content.SharedPreferences
import com.wendellugalds.kingofbozo.R

object ThemeStorage {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    fun saveTheme(context: Context, themeResId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, themeResId).apply()
    }

    fun getTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME, R.style.Theme_KingOfBozo) // Default theme
    }
}