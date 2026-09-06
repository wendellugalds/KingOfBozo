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
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.databinding.DialogExitGameBinding
import com.wendellugalds.kingofbozo.databinding.FragmentRankingBinding
import com.wendellugalds.kingofbozo.ui.game.adapter.RankingAtualAdapter
import com.wendellugalds.kingofbozo.util.SystemBarUtil
import com.wendellugalds.kingofbozo.util.AdManager

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var rankingAdapter: RankingAtualAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeGameState()
        setupButtons()
        setupOnBackPressed()
        configurarCoresDaBarra()
    }

    private fun configurarCoresDaBarra() {
        SystemBarUtil.applySystemBarColors(requireActivity().window, binding.root, statusBarAttr = R.attr.customBackground, navBarAttr = R.attr.customBackground)
    }

    private fun setupRecyclerViews() {
        rankingAdapter = RankingAtualAdapter()
        binding.recyclerViewListaJogadoresRankingRodadaFinal.apply {
            adapter = rankingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupButtons() {
        binding.btnRankingGeralRodada.setOnClickListener {
            AdManager.checkAndShowRankingAd(requireActivity(), parentFragmentManager) {
                val bottomSheet = RankingGeralBottomSheet()
                bottomSheet.show(childFragmentManager, "RankingGeralBottomSheet")
            }
        }

        binding.btnJogarMaisUm.setOnClickListener {
            AdManager.checkAndShowNextRoundAd(requireActivity(), parentFragmentManager) {
                gameViewModel.startNextRound()
                findNavController().navigateUp()
            }
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

        val navOptions = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.navigation_home, false)
            .build()

        dialogBinding.btnSave.setOnClickListener {
            gameViewModel.saveCurrentGame(requireContext())
            dialog.dismiss()
            if (gameViewModel.showPremiumLimitEvent.value == null) {
                findNavController().navigate(R.id.navigation_saved_games, null, navOptions)
            }
        }

        dialogBinding.btnExitNoSave.setOnClickListener {
            gameViewModel.discardUnsavedChanges()
            dialog.dismiss()
            findNavController().navigate(R.id.navigation_saved_games, null, navOptions)
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
                showExitConfirmationDialog()
            }
        })
    }

    private fun observeGameState() {
        gameViewModel.gameState.observe(viewLifecycleOwner) { state ->
            state?.let {
                binding.textTitleJogadores.text = "RODADA ${it.currentRound.toString().padStart(2, '0')}"

                // Ranking da rodada finalizada
                val sortedCurrent = it.playersState.sortedByDescending { p -> p.totalScore }
                rankingAdapter.submitList(sortedCurrent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
