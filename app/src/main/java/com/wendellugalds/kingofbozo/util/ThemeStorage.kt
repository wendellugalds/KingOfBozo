package com.wendellugalds.kingofbozo.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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
        updateAppIcon(context, themeName)
    }

    private fun updateAppIcon(context: Context, themeName: String) {
        val packageName = "com.wendellugalds.kingofbozo"
        
        val aliasName = when (themeName) {
            "PADRAO" -> "$packageName.SplashActivityPadrao"
            "VERDE" -> "$packageName.SplashActivityVerde"
            "ROXO" -> "$packageName.SplashActivityRoxo"
            "Rosa" -> "$packageName.SplashActivityRosa"
            "LARANJA" -> "$packageName.SplashActivityLaranja"
            "VERMELHO" -> "$packageName.SplashActivityVermelho"
            else -> "$packageName.SplashActivityPadrao"
        }

        val aliases = listOf(
            "$packageName.SplashActivityPadrao",
            "$packageName.SplashActivityVerde",
            "$packageName.SplashActivityRoxo",
            "$packageName.SplashActivityRosa",
            "$packageName.SplashActivityLaranja",
            "$packageName.SplashActivityVermelho"
        )

        val packageManager = context.packageManager

        aliases.forEach { alias ->
            val componentName = ComponentName(packageName, alias)
            val state = if (alias == aliasName) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            packageManager.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun getTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME_NAME, "PADRAO")
        return when (themeName) {
            "PADRAO" -> R.style.Base_Theme_KingOfBozo_Standard
            "VERDE" -> R.style.Base_Theme_KingOfBozo_Verde
            "ROXO" -> R.style.Base_Theme_KingOfBozo_Roxo
            "Rosa" -> R.style.Base_Theme_KingOfBozo_Rosa
            "LARANJA" -> R.style.Base_Theme_KingOfBozo_Laranja
            "VERMELHO" -> R.style.Base_Theme_KingOfBozo_Vermelho
            else -> R.style.Base_Theme_KingOfBozo_Standard
        }
    }

    fun getThemeKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME_NAME, "PADRAO") ?: "PADRAO"
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
