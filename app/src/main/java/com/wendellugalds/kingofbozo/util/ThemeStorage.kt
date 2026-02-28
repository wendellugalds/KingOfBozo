package com.wendellugalds.kingofbozo.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.wendellugalds.kingofbozo.R

object ThemeStorage {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_NAME = "selected_theme_name"
    private const val KEY_NIGHT_MODE = "night_mode"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"

    fun saveTheme(context: Context, themeName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_NAME, themeName).apply()
    }

    fun getTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME_NAME, "VERDE")
        return when (themeName) {
            "PADRAO" -> R.style.Base_Theme_KingOfBozo_Standard
            "VERDE" -> R.style.Base_Theme_KingOfBozo_verde
            "AZUL" -> R.style.Base_Theme_KingOfBozo_azul
            "ROXO" -> R.style.Base_Theme_KingOfBozo_roxo
            "PINK" -> R.style.Base_Theme_KingOfBozo_pink
            "AMARELO" -> R.style.Base_Theme_KingOfBozo_amarelo
            "LARANJA" -> R.style.Base_Theme_KingOfBozo_laranja
            "VERMELHO" -> R.style.Base_Theme_KingOfBozo_vermelho
            else -> R.style.Theme_KingOfBozo
        }
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

    fun saveKeepScreenOn(context: Context, keepOn: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, keepOn).apply()
    }

    fun getKeepScreenOn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
    }

    fun applySettings(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getNightMode(context))
    }
}
