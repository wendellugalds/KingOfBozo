package com.wendellugalds.kingofbozo.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.databinding.FragmentPlayerSelectionMarkerBinding
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.ui.AddPlayerBottomSheet
import com.wendellugalds.kingofbozo.ui.PremiumBottomSheet
import com.wendellugalds.kingofbozo.ui.game.adapter.PlayerSelectionAdapter
import com.wendellugalds.kingofbozo.ui.game.adapter.SelectablePlayerItem
import com.wendellugalds.kingofbozo.util.PremiumManager
import com.wendellugalds.kingofbozo.R

class PlayerSelectionFragment : Fragment() {

    private var _binding: FragmentPlayerSelectionMarkerBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var playerAdapter: PlayerSelectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerSelectionMarkerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Força a barra de status a assumir a cor primária exata do tema de forma opaca
        val window = requireActivity().window
        val colorPrimary = MaterialColors.getColor(binding.root, R.attr.colorPrimary)
        window.statusBarColor = colorPrimary
        window.navigationBarColor = colorPrimary

        // Afasta o cabeçalho para baixo da barra de status (relógio/bateria)
        ViewCompat.setOnApplyWindowInsetsListener(binding.actionBar) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight + 16
            }
            insets
        }

        gameViewModel.clearSelection()

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        playerAdapter = PlayerSelectionAdapter { player ->
            gameViewModel.togglePlayerSelection(player)
        }

        binding.recyclerViewPlayers.apply {
            adapter = playerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            gameViewModel.clearSelection()
            findNavController().navigateUp()
        }

        binding.buttonAdicionarJogador.setOnClickListener {
            val totalJogadores = gameViewModel.sortedPlayerListForSelection.value.orEmpty().size
            if (!PremiumManager.isUserPremium(requireContext()) && totalJogadores >= PremiumManager.MAX_FREE_PLAYERS) {
                val premiumSheet = PremiumBottomSheet()
                premiumSheet.show(parentFragmentManager, "PremiumBottomSheet")
            } else {
                val addPlayerSheet = AddPlayerBottomSheet()
                addPlayerSheet.show(parentFragmentManager, "AddPlayerSheet")
            }
        }

        binding.tirarSeleO.setOnClickListener {
            gameViewModel.clearSelection()
        }

        binding.iniciar.setOnClickListener {
            val selectedPlayers = gameViewModel.selectedPlayers.value ?: return@setOnClickListener
            if (selectedPlayers.size < 2) return@setOnClickListener

            gameViewModel.startGame()

            val selectedPlayerIds = selectedPlayers.map { it.id }.toLongArray()
            val action = PlayerSelectionFragmentDirections.actionPlayerSelectionFragmentToMarcadorFragment(selectedPlayerIds)
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        gameViewModel.sortedPlayerListForSelection.observe(viewLifecycleOwner) { allPlayers ->
            val selectedPlayers = gameViewModel.selectedPlayers.value.orEmpty()
            updateAdapterList(allPlayers, selectedPlayers)
            updateInfoText(allPlayers, selectedPlayers)

            val isEmpty = allPlayers.isNullOrEmpty()
            binding.imageEmptyState.isVisible = isEmpty
            binding.imageEmptyStateBack.isVisible = isEmpty
            binding.recyclerViewPlayers.isVisible = !isEmpty
        }

        gameViewModel.selectedPlayers.observe(viewLifecycleOwner) { selectedPlayers ->
            val allPlayers = gameViewModel.sortedPlayerListForSelection.value.orEmpty()
            updateAdapterList(allPlayers, selectedPlayers)
            updateInfoText(allPlayers, selectedPlayers)

            val selectionCount = selectedPlayers.size
            binding.iniciar.isVisible = selectionCount >= 2
            binding.buttonAdicionarJogador.isVisible = selectionCount == 0
            binding.tirarSeleO.isVisible = selectionCount > 0

            binding.infoText.text = when {
                selectionCount == 0 -> getString(R.string.nenhum_jogador_selecionado)
                selectionCount < 2 -> getString(R.string.selecione_pelo_menos_2)
                selectionCount == 9 -> getString(R.string.limite_9_jogadores)
                else -> getString(R.string.selecione_ate_x_jogadores, if (allPlayers.size > 9) 9 else allPlayers.size)
            }
        }
    }

    private fun updateInfoText(allPlayers: List<Player>, selectedPlayers: List<Player>) {
        val selectionCount = selectedPlayers.size
        val totalPlayersCount = allPlayers.size
        val maxSelectable = if (totalPlayersCount > 9) 9 else totalPlayersCount

        binding.infoText.isVisible = totalPlayersCount > 0
        binding.infoText.text = when {
            selectionCount == 0 -> getString(R.string.nenhum_jogador_selecionado)
            selectionCount < 2 -> getString(R.string.selecione_pelo_menos_2)
            selectionCount == 9 -> getString(R.string.limite_9_jogadores)
            else -> getString(R.string.selecione_ate_x_jogadores, maxSelectable)
        }
    }

    private fun updateAdapterList(allPlayers: List<Player>, selectedPlayers: List<Player>) {
        val selectionOrderMap = selectedPlayers.mapIndexed { index, player -> player.id to index + 1 }.toMap()
        val isMaxReached = selectedPlayers.size >= 9

        val selectableItems = allPlayers.filter { player ->
            selectionOrderMap.containsKey(player.id) || !isMaxReached
        }.map { player ->
            SelectablePlayerItem(
                player = player,
                isSelected = selectionOrderMap.containsKey(player.id),
                selectionOrder = selectionOrderMap[player.id]
            )
        }
        playerAdapter.submitList(selectableItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}