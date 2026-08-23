package com.wendellugalds.kingofbozo

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.databinding.ActivityMainBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun attachBaseContext(newBase: Context) {
        val newConfig = Configuration(newBase.resources.configuration)
        newConfig.fontScale = 1.0f
        applyOverrideConfiguration(newConfig)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeStorage.applySettings(this)
        setTheme(ThemeStorage.getTheme(this))
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarCoresDaBarra()
        applyKeepScreenOn(ThemeStorage.getKeepScreenOn(this))

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        setupCustomNavigation()
        setupNavigationVisibility()
    }

    private fun configurarCoresDaBarra() {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(R.attr.customBackground, typedValue, true)
        val bgColor = typedValue.data

        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
    }

    private fun applyKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun setupCustomNavigation() {
        binding.navHome.setOnClickListener { navController.navigate(R.id.navigation_home) }
        binding.navPlayers.setOnClickListener { navController.navigate(R.id.navigation_players) }
        binding.navSavedGames.setOnClickListener { navController.navigate(R.id.navigation_saved_games) }
        binding.navSettings.setOnClickListener { navController.navigate(R.id.navigation_settings) }
    }

    private fun setupNavigationVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isVisible = when (destination.id) {
                R.id.navigation_home,
                R.id.navigation_players,
                R.id.navigation_saved_games,
                R.id.navigation_settings -> true
                else -> false
            }
            binding.cardNavigation.visibility = if (isVisible) View.VISIBLE else View.GONE
            updateNavIcons(destination.id)

            val typedValuePrimary = android.util.TypedValue()
            theme.resolveAttribute(R.attr.colorPrimary, typedValuePrimary, true)
            val primaryColor = typedValuePrimary.data

            val typedValueBackground = android.util.TypedValue()
            theme.resolveAttribute(R.attr.customBackground, typedValueBackground, true)
            val backgroundColor = typedValueBackground.data

            // Altera diretamente a cor da barra de status e navegação baseada na tela ativa
            if (destination.id == R.id.playerSelectionFragment) {
                window.statusBarColor = primaryColor
                window.navigationBarColor = primaryColor
                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
            } else {
                window.statusBarColor = backgroundColor
                window.navigationBarColor = backgroundColor
                val isLight = typedValueBackground.data == Color.WHITE || isColorLight(backgroundColor)
                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
            }
        }
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness < 0.5
    }

    private fun updateNavIcons(activeDestinationId: Int) {
        val inactiveColor = MaterialColors.getColor(this, R.attr.customIcon, Color.GRAY)
        val activeColor = MaterialColors.getColor(this, R.attr.colorPrimary, Color.BLACK)
        val alpha = 0.10f
        val activeBgColor = Color.argb((alpha * 255).toInt(), Color.red(inactiveColor), Color.green(inactiveColor), Color.blue(inactiveColor))

        resetNavItem(binding.navHome, binding.navHomeIcon, binding.navHomeText, R.drawable.ic_home, inactiveColor)
        resetNavItem(binding.navPlayers, binding.navPlayersIcon, binding.navPlayersText, R.drawable.ic_players, inactiveColor)
        resetNavItem(binding.navSavedGames, binding.navSavedGamesIcon, binding.navSavedGamesText, R.drawable.ic_game, inactiveColor)
        resetNavItem(binding.navSettings, binding.navSettingsIcon, binding.navSettingsText, R.drawable.ic_settings, inactiveColor)

        when (activeDestinationId) {
            R.id.navigation_home -> setActiveNavItem(binding.navHome, binding.navHomeIcon, binding.navHomeText, R.drawable.ic_home_active, activeColor, activeBgColor)
            R.id.navigation_players -> setActiveNavItem(binding.navPlayers, binding.navPlayersIcon, binding.navPlayersText, R.drawable.ic_players_active, activeColor, activeBgColor)
            R.id.navigation_saved_games -> setActiveNavItem(binding.navSavedGames, binding.navSavedGamesIcon, binding.navSavedGamesText, R.drawable.ic_game_active, activeColor, activeBgColor)
            R.id.navigation_settings -> setActiveNavItem(binding.navSettings, binding.navSettingsIcon, binding.navSettingsText, R.drawable.ic_settings_active, activeColor, activeBgColor)
        }
    }

    private fun resetNavItem(layout: LinearLayout, icon: ImageView, text: TextView, iconRes: Int, color: Int) {
        layout.background = null
        icon.setImageResource(iconRes)
        icon.imageTintList = ColorStateList.valueOf(color)
        text.visibility = View.GONE
    }

    private fun setActiveNavItem(layout: LinearLayout, icon: ImageView, text: TextView, iconRes: Int, color: Int, bgColor: Int) {
        val shape = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 100f
            setColor(bgColor)
        }
        layout.background = shape
        icon.setImageResource(iconRes)
        icon.imageTintList = ColorStateList.valueOf(color)
        text.setTextColor(color)
        text.visibility = View.VISIBLE
    }
}