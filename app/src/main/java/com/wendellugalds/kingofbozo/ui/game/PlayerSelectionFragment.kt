package com.wendellugalds.kingofbozo.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.databinding.FragmentPlayerSelectionMarkerBinding
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.ui.AddPlayerBottomSheet
import com.wendellugalds.kingofbozo.ui.game.adapter.PlayerSelectionAdapter
import com.wendellugalds.kingofbozo.ui.game.adapter.SelectablePlayerItem
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

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        configurarCoresDaBarra()
    }
    private fun configurarCoresDaBarra() {
        val window = requireActivity().window
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)

        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoFundo

        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = false
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
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
            val addPlayerSheet = AddPlayerBottomSheet()
            addPlayerSheet.show(parentFragmentManager, "AddPlayerSheet")
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
        }

        gameViewModel.selectedPlayers.observe(viewLifecycleOwner) { selectedPlayers ->
            val allPlayers = gameViewModel.sortedPlayerListForSelection.value.orEmpty()
            updateAdapterList(allPlayers, selectedPlayers)
            updateInfoText(allPlayers, selectedPlayers)

            val selectionCount = selectedPlayers.size
            binding.iniciar.isVisible = selectionCount >= 2
            binding.buttonAdicionarJogador.isVisible = selectionCount == 0
            binding.tirarSeleO.isVisible = selectionCount > 0
            
            // Atualiza o texto informativo
            binding.infoText.text = when {
                selectionCount == 0 -> "Nenhum jogador selecionado"
                selectionCount < 2 -> "Selecione pelo menos 2 jogadores"
                selectionCount == 9 -> "Limite de 9 jogadores atingido"
                else -> "Selecione até ${if (allPlayers.size > 9) 9 else allPlayers.size} jogadores"
            }
        }
    }

    private fun updateInfoText(allPlayers: List<Player>, selectedPlayers: List<Player>) {
        val selectionCount = selectedPlayers.size
        val totalPlayersCount = allPlayers.size
        val maxSelectable = if (totalPlayersCount > 9) 9 else totalPlayersCount

        binding.infoText.isVisible = totalPlayersCount > 0
        binding.infoText.text = when {
            selectionCount == 0 -> "Nenhum jogador selecionado"
            selectionCount < 2 -> "Selecione pelo menos 2 jogadores"
            selectionCount == 9 -> "Limite de 9 jogadores atingido"
            else -> "Selecione até $maxSelectable jogadores"
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
