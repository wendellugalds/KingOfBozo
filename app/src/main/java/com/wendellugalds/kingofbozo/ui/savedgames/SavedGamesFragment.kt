package com.wendellugalds.kingofbozo.ui.savedgames

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentSavedGamesBinding
import com.wendellugalds.kingofbozo.databinding.DialogDeleteGameBinding
import com.wendellugalds.kingofbozo.ui.game.GameViewModel
import com.wendellugalds.kingofbozo.ui.game.GameViewModelFactory

class SavedGamesFragment : Fragment() {

    private var _binding: FragmentSavedGamesBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var savedGamesAdapter: SavedGamesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarCoresDaBarra()
        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.buttonMarcadorJogo.setOnClickListener {
            // Navega para a seleção de jogadores, assim como o botão da Home
            findNavController().navigate(R.id.action_global_playerSelectionFragment)
        }
    }

    private fun setupRecyclerView() {
        savedGamesAdapter = SavedGamesAdapter(
            onClick = { savedGame ->
                gameViewModel.loadGame(savedGame)
                findNavController().navigate(R.id.action_navigation_saved_games_to_marcadorFragment)
            },
            onDelete = { savedGame ->
                showDeleteConfirmationDialog(savedGame)
            }
        )
        binding.recyclerViewJogosSalvos.apply {
            adapter = savedGamesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showDeleteConfirmationDialog(savedGame: com.wendellugalds.kingofbozo.model.SavedGame) {
        val dialogBinding = DialogDeleteGameBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            gameViewModel.deleteSavedGame(savedGame)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Partida apagada!", Toast.LENGTH_SHORT).show()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun observeViewModel() {
        gameViewModel.allSavedGames.observe(viewLifecycleOwner) { games ->
            savedGamesAdapter.submitList(games)
        }
    }

    private fun configurarCoresDaBarra() {
        val window = requireActivity().window
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.background)
        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoFundo
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_NO
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}