package com.wendellugalds.kingofbozo.ui.playerdetail

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.wendellugalds.kingofbozo.PlayersApplication
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.FragmentPlayerDetailBinding
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.ui.EditPlayerBottomSheet
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModel
import com.wendellugalds.kingofbozo.ui.players.PlayerViewModelFactory


class PlayerDetailFragment : Fragment() {

    private var _binding: FragmentPlayerDetailBinding? = null
    private val binding get() = _binding!!


    private val playerViewModel: PlayerViewModel by activityViewModels {
        PlayerViewModelFactory((requireActivity().application as PlayersApplication).repository)
    }

    private val args: PlayerDetailFragmentArgs by navArgs()
    private var currentPlayer: Player? = null

    // A lógica de escolher imagem foi REMOVIDA daqui

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playerId = args.playerId.toLong()

        playerViewModel.getPlayerById(playerId).observe(viewLifecycleOwner) { player ->
            if (player != null) {
                currentPlayer = player
                bind(player)
            } else {
                // Se o jogador for nulo (ex: foi deletado), volta para a tela anterior
                findNavController().navigateUp()
            }
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.buttonEdit.setOnClickListener {
            currentPlayer?.let {
                val editSheet = EditPlayerBottomSheet.newInstance(it.id)
                editSheet.show(parentFragmentManager, "EditPlayerSheet")
            }
        }

        // Os cliques na imagem foram REMOVIDOS daqui
    }

    private fun showDeleteConfirmationDialog() {
        currentPlayer?.let { playerToDelete ->
            AlertDialog.Builder(requireContext())
                .setTitle("Apagar Jogador")
                .setMessage("Tem certeza que deseja apagar ${playerToDelete.name}?")
                .setPositiveButton("Apagar") { _, _ ->
                    playerViewModel.deletePlayers(listOf(playerToDelete))
                    Toast.makeText(requireContext(), "${playerToDelete.name} foi apagado.", Toast.LENGTH_SHORT).show()
                    // findNavController().navigateUp() é chamado automaticamente pelo observer
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun bind(player: Player) {
        binding.textPlayerNameDetail.text = player.name
        binding.textRankingWins.text = player.wins.toString()
        binding.textIdade.text = "${player.age} aninhos"

        val losses = player.totalRounds - player.wins

        if (!player.imageUri.isNullOrEmpty()) {
            // CASO 1: TEM IMAGEM
            // Mostra o ImageView
            binding.imagePlayerAvatarDetail.visibility = View.VISIBLE
            binding.imagePlayerAvatarDetail.load(Uri.parse(player.imageUri))

            // Esconde o TextView das iniciais
            binding.siglaNome.visibility = View.GONE
        } else {
            // CASO 2: NÃO TEM IMAGEM
            // Esconde o ImageView (essa é a mudança principal)
            binding.imagePlayerAvatarDetail.visibility = View.GONE

            // Mostra o TextView das iniciais
            binding.siglaNome.visibility = View.VISIBLE

            // Sua lógica para as iniciais (que já estava correta)
            val name = player.name?.trim() ?: ""
            val words = name.split(" ").filter { it.isNotBlank() }
            if (words.size > 1) {
                val firstInitial = words.first().first()
                val lastInitial = words.last().first()
                binding.siglaNome.text = "$firstInitial$lastInitial"
            } else if (words.isNotEmpty()) {
                val word = words.first()
                binding.siglaNome.text = if (word.length >= 2) word.substring(0, 2) else word
            } else {
                binding.siglaNome.text = "--"
            }
        }

        binding.textLoseValue.text = losses.toString()
        binding.textJogadasValue.text = player.totalRounds.toString()

        if (player.totalRounds > 0) {
            binding.progressStats.max = player.totalRounds
            binding.progressStats.progress = player.wins
        } else {
            binding.progressStats.progress = 0
            binding.progressStats.max = 1
        }

        binding.textStatGenerais.text = getString(R.string.generais_conquistados_format, player.generals)
        binding.textStatBocas.text = getString(R.string.jogadas_de_bocas_format, player.mouthPlays)
        binding.textStatRodadas.text = getString(R.string.rodadas_feitas_format, player.totalRounds)
        binding.textLoseValue.text = losses.toString()
        binding.textPontosGerais.text = "${player.totalRounds} Rodadas"

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}