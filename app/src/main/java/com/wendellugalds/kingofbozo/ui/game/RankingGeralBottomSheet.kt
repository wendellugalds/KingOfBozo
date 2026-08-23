package com.wendellugalds.kingofbozo.ui.game

import android.app.Dialog
import android.content.DialogInterface
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
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.util.SystemBarUtil
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.databinding.BottomSheetRankingGeralBinding
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.ui.game.adapter.RankingGeralAdapter

class RankingGeralBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRankingGeralBinding? = null
    private val binding get() = _binding!!

    private var originalStatusBarColor: Int = 0
    private var originalNavBarColor: Int = 0

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var rankingGeralAdapter: RankingGeralAdapter

    override fun onStart() {
        super.onStart()
        originalStatusBarColor = requireActivity().window.statusBarColor
        originalNavBarColor = requireActivity().window.navigationBarColor
        SystemBarUtil.applySystemBarColors(
            requireActivity().window,
            binding.root,
            statusBarAttr = R.attr.customBackground,
            navBarAttr = R.attr.colorPrimary
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        SystemBarUtil.setSystemBarColors(
            requireActivity().window,
            requireActivity().findViewById(android.R.id.content),
            originalStatusBarColor,
            originalNavBarColor
        )
    }

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
        return dialog
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
