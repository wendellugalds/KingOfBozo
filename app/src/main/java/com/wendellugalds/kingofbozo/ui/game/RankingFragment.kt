package com.wendellugalds.kingofbozo.ui.game

import android.net.Uri
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
import coil.load
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.DialogExitGameBinding
import com.wendellugalds.kingofbozo.databinding.FragmentRankingBinding
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.ui.game.adapter.RankingAtualAdapter
import com.wendellugalds.kingofbozo.util.AdManager
import com.wendellugalds.kingofbozo.util.SystemBarUtil

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
        rankingAdapter = RankingAtualAdapter(isDuranteJogo = true)
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
                val sortedPlayers = it.playersState.sortedByDescending { p -> p.totalScore }

                // 1º Lugar (podio_1)
                if (sortedPlayers.isNotEmpty()) {
                    binding.podio1.visibility = View.VISIBLE
                    bindPodium1(sortedPlayers[0])
                } else {
                    binding.podio1.visibility = View.GONE
                }

                // 2º Lugar (podio_2)
                if (sortedPlayers.size >= 2) {
                    binding.podio2.visibility = View.VISIBLE
                    bindPodium2(sortedPlayers[1])
                } else {
                    binding.podio2.visibility = View.GONE
                }

                // 3º Lugar (podio_3) - Esconde caso só tenha 2 jogadores
                if (sortedPlayers.size >= 3) {
                    binding.podio3.visibility = View.VISIBLE
                    bindPodium3(sortedPlayers[2])
                } else {
                    binding.podio3.visibility = View.GONE
                }

                // Lista a partir do 4º jogador (se houver mais que 3 jogadores)
                if (sortedPlayers.size > 3) {
                    binding.recyclerViewListaJogadoresRankingRodadaFinal.visibility = View.VISIBLE
                    rankingAdapter.submitList(sortedPlayers.subList(3, sortedPlayers.size))
                } else {
                    binding.recyclerViewListaJogadoresRankingRodadaFinal.visibility = View.GONE
                    rankingAdapter.submitList(emptyList())
                }
            }
        }
    }

    private fun formatPlayerName(fullName: String): String {
        val words = fullName.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return if (words.size >= 2) {
            "${words[0]} ${words[1]}"
        } else {
            fullName.trim()
        }
    }

    private fun bindPodium1(player: PlayerState) {
        binding.textNamePodio1.text = formatPlayerName(player.playerName)
        binding.textScorePodio1.text = "${player.totalScore} Pontos"

        if (!player.playerImage.isNullOrEmpty()) {
            binding.imageAvatarPodio1.visibility = View.VISIBLE
            binding.siglaNomePodio1.visibility = View.GONE
            binding.imageAvatarPodio1.load(Uri.parse(player.playerImage))
        } else {
            binding.imageAvatarPodio1.visibility = View.GONE
            binding.siglaNomePodio1.visibility = View.VISIBLE
            binding.siglaNomePodio1.text = player.playerName.take(2).uppercase()
        }
    }

    private fun bindPodium2(player: PlayerState) {
        binding.textNamePodio2.text = formatPlayerName(player.playerName)
        binding.textScorePodio2.text = "${player.totalScore} Pontos"

        if (!player.playerImage.isNullOrEmpty()) {
            binding.imageAvatarPodio2.visibility = View.VISIBLE
            binding.siglaNomePodio2.visibility = View.GONE
            binding.imageAvatarPodio2.load(Uri.parse(player.playerImage))
        } else {
            binding.imageAvatarPodio2.visibility = View.GONE
            binding.siglaNomePodio2.visibility = View.VISIBLE
            binding.siglaNomePodio2.text = player.playerName.take(2).uppercase()
        }
    }

    private fun bindPodium3(player: PlayerState) {
        binding.textNamePodio3.text = formatPlayerName(player.playerName)
        binding.textScorePodio3.text = "${player.totalScore} Pontos"

        if (!player.playerImage.isNullOrEmpty()) {
            binding.imageAvatarPodio3.visibility = View.VISIBLE
            binding.siglaNomePodio3.visibility = View.GONE
            binding.imageAvatarPodio3.load(Uri.parse(player.playerImage))
        } else {
            binding.imageAvatarPodio3.visibility = View.GONE
            binding.siglaNomePodio3.visibility = View.VISIBLE
            binding.siglaNomePodio3.text = player.playerName.take(2).uppercase()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}