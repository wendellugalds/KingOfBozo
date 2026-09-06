package com.wendellugalds.kingofbozo.ui.savedgames

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentSavedGamesBinding
import com.wendellugalds.kingofbozo.model.SavedGame
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.ui.game.GameViewModel
import com.wendellugalds.kingofbozo.ui.game.GameViewModelFactory
import com.wendellugalds.kingofbozo.util.SystemBarUtil
import com.wendellugalds.kingofbozo.util.AnimationUtil
import com.wendellugalds.kingofbozo.util.PremiumManager
import com.wendellugalds.kingofbozo.ui.PremiumBottomSheet
import com.wendellugalds.kingofbozo.ui.game.GameLoadingActivity
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SavedGamesFragment : Fragment() {

    private var _binding: FragmentSavedGamesBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var savedGamesAdapter: SavedGamesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeSavedGames()
        setupClickListeners()
        configurarCoresDaBarra()
        
        binding.buttonMarcadorJogo.post {
            AnimationUtil.applyExpansionAnimation(binding.buttonMarcadorJogo)
        }
    }

    private fun configurarCoresDaBarra() {
        SystemBarUtil.applySystemBarColors(requireActivity().window, binding.root, statusBarAttr = R.attr.customBackground, navBarAttr = R.attr.cardBackgroundColor)
    }

    private fun setupClickListeners() {
        binding.buttonMarcadorJogo.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val count = gameViewModel.getSavedGamesCount()
                if (!PremiumManager.isUserPremium(requireContext()) && count >= PremiumManager.MAX_FREE_SAVED_GAMES) {
                    val premiumSheet = PremiumBottomSheet()
                    premiumSheet.show(parentFragmentManager, "PremiumBottomSheet")
                } else {
                    AnimationUtil.applyCollapseAnimation(binding.buttonMarcadorJogo) {
                        findNavController().navigate(R.id.action_global_playerSelectionFragment)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        savedGamesAdapter = SavedGamesAdapter(
            onClick = { savedGame ->
                AnimationUtil.collapseAnyExpandedButton(binding.root) {
                    val intent = Intent(requireContext(), GameLoadingActivity::class.java)
                    intent.putExtra("GAME_ID", savedGame.id)
                    startActivity(intent)
                    requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            },
            onDelete = { savedGame ->
                showDeleteConfirmationDialog(savedGame)
            },
            onGamersClick = { savedGame ->
                showSavedGamePlayersDialog(savedGame)
            }
        )
        binding.recyclerViewJogosSalvos.apply {
            adapter = savedGamesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showSavedGamePlayersDialog(savedGame: SavedGame) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_saved_game_players, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        val textTitle = dialogView.findViewById<TextView>(R.id.text_title)
        val textInstructions = dialogView.findViewById<TextView>(R.id.text_instructions)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_saved_game_players)

        val gson = Gson()
        val listType = object : TypeToken<List<PlayerState>>() {}.type
        val playerStates: List<PlayerState> = gson.fromJson(savedGame.playerStatesJson, listType)

        // Se houver pontos marcados na rodada, ordena pela pontuação. Caso contrário (0 pontos), mantém a ordem de jogada original.
        val hasScores = playerStates.any { it.totalScore > 0 }
        val sortedPlayers = if (hasScores) {
            playerStates.sortedByDescending { it.totalScore }
        } else {
            playerStates
        }

        val adapter = SavedGamePlayersAdapter(savedGame.currentRound)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        adapter.submitList(sortedPlayers)

        textTitle.text = "LISTA DE JOGADORES"
        textInstructions.text = "${playerStates.size} jogadores nesse jogo"

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeSavedGames() {
        gameViewModel.allSavedGames.observe(viewLifecycleOwner) { games ->
            savedGamesAdapter.submitList(games)
            
            // Controle do Empty State para Jogos Salvos
            val isEmpty = games.isNullOrEmpty()
            binding.imageEmptyState.isVisible = isEmpty
            binding.imageEmptyStateBack.isVisible = isEmpty
            binding.recyclerViewJogosSalvos.isVisible = !isEmpty
        }
    }

    private fun showDeleteConfirmationDialog(savedGame: SavedGame) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_game, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val message = dialogView.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btn_delete)

        title.text = "Apagar Jogo"
        message.text = "Tem certeza que deseja apagar este jogo salvo?"

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            gameViewModel.deleteSavedGame(savedGame)
            Toast.makeText(requireContext(), "Jogo apagado.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
