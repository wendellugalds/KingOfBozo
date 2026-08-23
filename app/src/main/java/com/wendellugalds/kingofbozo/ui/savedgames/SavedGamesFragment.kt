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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentSavedGamesBinding
import com.wendellugalds.kingofbozo.model.SavedGame
import com.wendellugalds.kingofbozo.ui.game.GameViewModel
import com.wendellugalds.kingofbozo.ui.game.GameViewModelFactory
import com.wendellugalds.kingofbozo.util.SystemBarUtil

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
    }

    private fun configurarCoresDaBarra() {
        SystemBarUtil.applySystemBarColors(requireActivity().window, binding.root, statusBarAttr = R.attr.customBackground, navBarAttr = R.attr.cardBackgroundColor)
    }

    private fun setupClickListeners() {
        binding.buttonMarcadorJogo.setOnClickListener {
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
