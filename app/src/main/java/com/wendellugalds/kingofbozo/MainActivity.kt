package com.wendellugalds.kingofbozo

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.wendellugalds.kingofbozo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        setupCustomNavigation()
        setupFab()
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

    // --- FUNÇÃO CORRIGIDA ---
    private fun setupFab() {
        // Corrigido para usar o ID 'fab_marcador' do seu XML
        binding.fabMarcador.setOnClickListener {
            // Navega para a nova tela de seleção de jogadores usando a ação global
            navController.navigate(R.id.action_global_playerSelectionFragment)
        }
    }

    private fun setupNavigationVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Esconde a barra de navegação na nova tela de seleção e na de detalhes
            val isVisible = when (destination.id) {
                R.id.navigation_home,
                R.id.navigation_players,
                R.id.navigation_saved_games,
                R.id.navigation_settings -> true
                else -> false // Esconderá em playerSelectionFragment e navigation_player_detail
            }
            binding.cardNavigation.visibility = if (isVisible) View.VISIBLE else View.GONE
            updateNavIcons(destination.id)
        }
    }

    private fun updateNavIcons(activeDestinationId: Int) {
        val inactiveColor = ContextCompat.getColor(this, android.R.color.white)
        val activeColor = ContextCompat.getColor(this, R.color.cor_destaque)

        binding.navHome.setImageResource(R.drawable.ic_home)
        binding.navHome.imageTintList = ColorStateList.valueOf(inactiveColor)

        binding.navPlayers.setImageResource(R.drawable.ic_players)
        binding.navPlayers.imageTintList = ColorStateList.valueOf(inactiveColor)

        binding.navSavedGames.setImageResource(R.drawable.ic_saved_games)
        binding.navSavedGames.imageTintList = ColorStateList.valueOf(inactiveColor)

        binding.navSettings.setImageResource(R.drawable.ic_settings)
        binding.navSettings.imageTintList = ColorStateList.valueOf(inactiveColor)

        when (activeDestinationId) {
            R.id.navigation_home -> {
                binding.navHome.setImageResource(R.drawable.ic_home_active)
                binding.navHome.imageTintList = ColorStateList.valueOf(activeColor)
            }
            R.id.navigation_players -> {
                binding.navPlayers.setImageResource(R.drawable.ic_players_active)
                binding.navPlayers.imageTintList = ColorStateList.valueOf(activeColor)
            }
            R.id.navigation_saved_games -> {
                binding.navSavedGames.setImageResource(R.drawable.ic_remenber_active)
                binding.navSavedGames.imageTintList = ColorStateList.valueOf(activeColor)
            }
            R.id.navigation_settings -> {
                binding.navSettings.setImageResource(R.drawable.ic_settings_active)
                binding.navSettings.imageTintList = ColorStateList.valueOf(activeColor)
            }
        }
    }
}