package com.wendellugalds.kingofbozo

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.databinding.ActivityMainBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeStorage.getTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        setupCustomNavigation()
        setupNavigationVisibility()
    }



    private fun setupCustomNavigation() {
        binding.navHome.setOnClickListener {
            navController.navigate(R.id.navigation_home)
        }
        binding.navPlayers.setOnClickListener {
            navController.navigate(R.id.navigation_players)
        }
        binding.navSavedGames.setOnClickListener {
            navController.navigate(R.id.navigation_saved_games)
        }
        binding.navSettings.setOnClickListener {
            navController.navigate(R.id.navigation_settings)
        }
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
        }
    }

    private fun updateNavIcons(activeDestinationId: Int) {
        val inactiveColor = MaterialColors.getColor(this, com.google.android.material.R.attr.icon, Color.GRAY)
        val activeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
        
        val alpha = 0.10f
//        val activeBgColor = inactiveColor
        val activeBgColor = Color.argb(
            (alpha * 255).toInt(),
            Color.red(activeColor),
            Color.green(activeColor),
            Color.blue(activeColor)
        )

        // Reset all to inactive state
        resetNavItem(binding.navHome, binding.navHomeIcon, binding.navHomeText, R.drawable.ic_home, inactiveColor)
        resetNavItem(binding.navPlayers, binding.navPlayersIcon, binding.navPlayersText, R.drawable.ic_players, inactiveColor)
        resetNavItem(binding.navSavedGames, binding.navSavedGamesIcon, binding.navSavedGamesText, R.drawable.ic_game, inactiveColor)
        resetNavItem(binding.navSettings, binding.navSettingsIcon, binding.navSettingsText, R.drawable.ic_settings, inactiveColor)

        // Set active state
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