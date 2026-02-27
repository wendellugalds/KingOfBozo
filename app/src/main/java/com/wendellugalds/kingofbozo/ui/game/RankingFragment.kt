package com.wendellugalds.kingofbozo.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.DialogExitGameBinding
import com.wendellugalds.kingofbozo.databinding.FragmentRankingBinding
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.ui.game.adapter.RankingAtualAdapter
import com.wendellugalds.kingofbozo.ui.game.adapter.RankingGeralAdapter

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var rankingAdapter: RankingAtualAdapter
    private lateinit var rankingGeralAdapter: RankingGeralAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupBottomSheet()
        observeGameState()
        setupButtons()
        setupOnBackPressed()
        configurarCoresDaBarra()
    }

    private fun configurarCoresDaBarra() {
        val window = requireActivity().window
        val corDoFundo = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.background)
        val corDoFundoBottomSheet = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
        window.statusBarColor = corDoFundo
        window.navigationBarColor = corDoFundoBottomSheet
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, binding.root)
        val isLightBackground = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_NO
        controller.isAppearanceLightStatusBars = isLightBackground
        controller.isAppearanceLightNavigationBars = isLightBackground
    }

    private fun setupRecyclerViews() {
        rankingAdapter = RankingAtualAdapter()
        binding.recyclerViewListaJogadoresRankingRodadaFinal.apply {
            adapter = rankingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        rankingGeralAdapter = RankingGeralAdapter()
        binding.recyclerViewListaJogadoresRankingGeralJogoAtual.apply {
            adapter = rankingGeralAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.persistentBottomSheet)
        
        binding.cardRankingGeral.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun setupButtons() {
        binding.btnJogarMaisUm.setOnClickListener {
            gameViewModel.startNextRound()
            findNavController().navigateUp()
        }

        binding.btnSair.setOnClickListener {
            showExitConfirmationDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        val dialogBinding = DialogExitGameBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnSave.setOnClickListener {
            gameViewModel.startNextRound()
            gameViewModel.saveCurrentGame()
            dialog.dismiss()
            findNavController().popBackStack(R.id.navigation_home, false)
        }

        dialogBinding.btnExitNoSave.setOnClickListener {
            gameViewModel.startNextRound()
            dialog.dismiss()
            findNavController().popBackStack(R.id.navigation_home, false)
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    showExitConfirmationDialog()
                }
            }
        })
    }

    private fun observeGameState() {
        gameViewModel.gameState.observe(viewLifecycleOwner) { state ->
            state?.let {
                binding.textTitleJogadores.text = "RODADA ${it.currentRound.toString().padStart(2, '0')}"
                binding.totalRodadas.text = "${it.currentRound} Rodadas"

                // Ranking da rodada finalizada
                val sortedCurrent = it.playersState.sortedByDescending { p -> p.totalScore }
                rankingAdapter.submitList(sortedCurrent)

                // Ranking Geral (Sessão) - Apenas jogadores com 1 ou mais vitórias
                val sortedGeral = it.playersState.sortedWith(
                    compareByDescending<PlayerState> { p -> p.sessionWins }
                        .thenByDescending { p -> p.sessionTotalPoints }
                ).filter { p -> p.sessionWins >= 1 }
                
                rankingGeralAdapter.submitList(sortedGeral)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
