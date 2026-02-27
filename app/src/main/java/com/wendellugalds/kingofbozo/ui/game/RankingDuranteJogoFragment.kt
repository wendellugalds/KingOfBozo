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
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentRankingDuranteJogoBinding
import com.wendellugalds.kingofbozo.ui.game.adapter.RankingAtualAdapter

class RankingDuranteJogoFragment : Fragment() {

    private var _binding: FragmentRankingDuranteJogoBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        GameViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private lateinit var rankingAdapter: RankingAtualAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRankingDuranteJogoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeGameState()
        setupButtons()
    }

    private fun setupRecyclerView() {
        // Inicializa o adapter passando true para indicar que estamos na tela DURANTE o jogo
        rankingAdapter = RankingAtualAdapter(isDuranteJogo = true)
        binding.recyclerViewListaJogadoresRankingJogoAtual.apply {
            adapter = rankingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeGameState() {
        gameViewModel.gameState.observe(viewLifecycleOwner) { state ->
            state?.let {
                binding.textTitleJogadores.text = "RODADA ${it.currentRound.toString().padStart(2, '0')}"
                val sortedPlayers = it.playersState.sortedByDescending { p -> p.totalScore }
                rankingAdapter.submitList(sortedPlayers)

                // Regra: O botão Ranking Geral só aparece se houver jogadores com 1 vitória ou mais
                val hasRankingGeral = it.playersState.any { player -> player.sessionWins >= 1 }
                binding.btnRakingGeral.isVisible = hasRankingGeral
            }
        }
    }

    private fun setupButtons() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnRakingGeral.setOnClickListener {
            showRankingGeralBottomSheet()
        }
    }

    private fun showRankingGeralBottomSheet() {
        val bottomSheet = RankingGeralBottomSheet()
        bottomSheet.show(childFragmentManager, "RankingGeralBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
