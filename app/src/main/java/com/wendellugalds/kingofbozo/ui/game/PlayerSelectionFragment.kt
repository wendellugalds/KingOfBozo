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
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.databinding.FragmentPlayerSelectionMarkerBinding
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.ui.AddPlayerBottomSheet
import com.wendellugalds.kingofbozo.ui.game.adapter.PlayerSelectionAdapter
import com.wendellugalds.kingofbozo.ui.game.adapter.SelectablePlayerItem

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
        }

        gameViewModel.selectedPlayers.observe(viewLifecycleOwner) { selectedPlayers ->
            val allPlayers = gameViewModel.sortedPlayerListForSelection.value.orEmpty()
            updateAdapterList(allPlayers, selectedPlayers)

            val selectionCount = selectedPlayers.size
            binding.iniciar.isVisible = selectionCount >= 2
            binding.buttonAdicionarJogador.isVisible = selectionCount == 0
        }
    }

    private fun updateAdapterList(allPlayers: List<Player>, selectedPlayers: List<Player>) {
        val selectionOrderMap = selectedPlayers.mapIndexed { index, player -> player.id to index + 1 }.toMap()

        val selectableItems = allPlayers.map { player ->
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