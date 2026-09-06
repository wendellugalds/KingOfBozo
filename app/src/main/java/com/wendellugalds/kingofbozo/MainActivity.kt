package com.wendellugalds.kingofbozo

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.navigation.NavController
import androidx.activity.viewModels
import com.wendellugalds.kingofbozo.ui.game.GameViewModel
import com.wendellugalds.kingofbozo.ui.game.GameViewModelFactory
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.databinding.ActivityMainBinding
import com.wendellugalds.kingofbozo.util.ThemeStorage
import com.wendellugalds.kingofbozo.util.SystemBarUtil
import com.wendellugalds.kingofbozo.util.AnimationUtil

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val gameViewModel: GameViewModel by viewModels {
        GameViewModelFactory((application as PlayersApplication).repository)
    }

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { _, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.container.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val gameId = intent?.getLongExtra("LOAD_GAME_ID", -1) ?: -1
        if (gameId != -1L) {
            val observer = object : androidx.lifecycle.Observer<List<com.wendellugalds.kingofbozo.model.SavedGame>> {
                override fun onChanged(value: List<com.wendellugalds.kingofbozo.model.SavedGame>) {
                    val game = value.find { it.id == gameId }
                    if (game != null) {
                        gameViewModel.loadGame(game)
                        gameViewModel.allSavedGames.removeObserver(this)

                        gameViewModel.gameState.observe(this@MainActivity, object : androidx.lifecycle.Observer<com.wendellugalds.kingofbozo.model.GameState?> {
                            override fun onChanged(value: com.wendellugalds.kingofbozo.model.GameState?) {
                                if (value != null) {
                                    val destination = if (gameViewModel.navigateToRanking.value == true) {
                                        R.id.rankingFragment
                                    } else {
                                        R.id.marcadorFragment
                                    }
                                    navController.navigate(destination)
                                    gameViewModel.gameState.removeObserver(this)
                                }
                            }
                        })
                    }
                }
            }
            gameViewModel.allSavedGames.observe(this, observer)
        }
    }

    private fun configurarCoresDaBarra() {
        SystemBarUtil.applySystemBarColors(
            window,
            binding.root,
            statusBarAttr = R.attr.customBackground,
            navBarAttr = R.attr.customBackground
        )
    }

    private fun applyKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun setupCustomNavigation() {
        val navOptions = androidx.navigation.NavOptions.Builder()
            .setEnterAnim(android.R.anim.fade_in)
            .setExitAnim(android.R.anim.fade_out)
            .setPopEnterAnim(android.R.anim.fade_in)
            .setPopExitAnim(android.R.anim.fade_out)
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .build()

        binding.navHome.setOnClickListener {
            if (navController.currentDestination?.id != R.id.navigation_home) {
                AnimationUtil.collapseAnyExpandedButton(window.decorView) {
                    navController.navigate(R.id.navigation_home, null, navOptions)
                }
            }
        }
        binding.navPlayers.setOnClickListener {
            if (navController.currentDestination?.id != R.id.navigation_players) {
                AnimationUtil.collapseAnyExpandedButton(window.decorView) {
                    navController.navigate(R.id.navigation_players, null, navOptions)
                }
            }
        }
        binding.navSavedGames.setOnClickListener {
            if (navController.currentDestination?.id != R.id.navigation_saved_games) {
                AnimationUtil.collapseAnyExpandedButton(window.decorView) {
                    navController.navigate(R.id.navigation_saved_games, null, navOptions)
                }
            }
        }
        binding.navSettings.setOnClickListener {
            if (navController.currentDestination?.id != R.id.navigation_settings) {
                AnimationUtil.collapseAnyExpandedButton(window.decorView) {
                    navController.navigate(R.id.navigation_settings, null, navOptions)
                }
            }
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
            
            // Animação suave para as mudanças no menu (ex: expansão do texto)
            val transition = AutoTransition().apply {
                duration = 250
                ordering = androidx.transition.TransitionSet.ORDERING_TOGETHER
            }
            TransitionManager.beginDelayedTransition(binding.bottomNavigationContainer, transition)
            
            updateNavIcons(destination.id)

            // Garante que as cores das barras de sistema (Status e Navigation) coincidam EXATAMENTE com a cor de fundo de cada tela
            when (destination.id) {
                R.id.playerSelectionFragment -> {
                    SystemBarUtil.applySystemBarColors(
                        window,
                        binding.root,
                        statusBarAttr = R.attr.colorPrimary,
                        navBarAttr = R.attr.colorPrimary
                    )
                }
                R.id.marcadorFragment -> {
                    SystemBarUtil.applySystemBarColors(
                        window,
                        binding.root,
                        statusBarAttr = R.attr.cardBackgroundColor,
                        navBarAttr = R.attr.cardBackgroundColor
                    )
                }
                else -> {
                    SystemBarUtil.applySystemBarColors(
                        window,
                        binding.root,
                        statusBarAttr = R.attr.customBackground,
                        navBarAttr = R.attr.customBackground
                    )
                }
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

        // Animação de escala no ícone ao ativar
        icon.scaleX = 0.8f
        icon.scaleY = 0.8f
        icon.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                icon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            .start()

        // Define o tamanho da fonte em sp sem dar erro
        text.textSize = 12f

        // Converte 12dp em pixels reais para o padding lateral não estourar o limite
        val paddingPx = (12 * resources.displayMetrics.density).toInt()
        layout.setPadding(paddingPx, 0, paddingPx, 0)

        text.visibility = View.VISIBLE
    }
}