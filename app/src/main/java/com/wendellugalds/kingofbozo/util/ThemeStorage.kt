package com.wendellugalds.kingofbozo.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.wendellugalds.kingofbozo.R

object ThemeStorage {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_NIGHT_MODE = "night_mode"

    fun saveTheme(context: Context, themeResId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, themeResId).apply()
    }

    fun getTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME, R.style.Theme_KingOfBozo)
    }

    fun saveNightMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getNightMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun applySettings(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getNightMode(context))
    }
}
