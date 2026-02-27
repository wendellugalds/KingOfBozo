package com.wendellugalds.kingofbozo.ui.game

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.databinding.BottomSheetRankingGeralBinding
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.ui.game.adapter.RankingGeralAdapter

class RankingGeralBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRankingGeralBinding? = null
    private val binding get() = _binding!!
    private var originalNavBarColor: Int = 0
    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var rankingGeralAdapter: RankingGeralAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetRankingGeralBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.setBackgroundResource(android.R.color.transparent)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        onStart()
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                originalNavBarColor = requireActivity().window.navigationBarColor
                val corDoTema = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorPrimary)
                window.navigationBarColor = corDoTema
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeGameState()
    }

    private fun setupRecyclerView() {
        rankingGeralAdapter = RankingGeralAdapter()
        binding.recyclerViewListaJogadoresRankingGeralJogoAtual.apply {
            adapter = rankingGeralAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeGameState() {
        gameViewModel.gameState.observe(viewLifecycleOwner) { state ->
            state?.let {
                binding.totalRodadas.text = "${it.currentRound} Rodadas"
                
                // Ordenar por vitórias na sessão e depois por pontos totais na sessão.
                // Filtra para mostrar apenas jogadores com 1 vitória ou mais.
                val sortedGeral = it.playersState.sortedWith(
                    compareByDescending<PlayerState> { p -> p.sessionWins }
                        .thenByDescending { p -> p.sessionTotalPoints }
                ).filter { it.sessionWins >= 1 }
                
                rankingGeralAdapter.submitList(sortedGeral)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
