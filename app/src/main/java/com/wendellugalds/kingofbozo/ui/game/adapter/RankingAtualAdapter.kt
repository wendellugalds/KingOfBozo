package com.wendellugalds.kingofbozo.ui.game.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ItemPlayerRodadaRankingBinding
import com.wendellugalds.kingofbozo.model.PlayerState

class RankingAtualAdapter(private val isDuranteJogo: Boolean = false) : ListAdapter<PlayerState, RankingAtualAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPlayerRodadaRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val player = getItem(position)
        
        val maxScore = currentList.maxOfOrNull { it.totalScore } ?: 0
        val winnersCount = currentList.count { it.totalScore == maxScore }
        
        // Um jogador está empatado se houver mais de um jogador com a mesma pontuação na lista.
        val countWithSameScore = currentList.count { it.totalScore == player.totalScore }
        val isEmpate = countWithSameScore > 1
        
        // O King (Coroa) só aparece se o jogador tiver a pontuação máxima e for o único líder.
        val isKing = player.totalScore == maxScore && winnersCount == 1

        // O layout colorido (theme_king) é aplicado tanto para o líder isolado quanto para qualquer empate.
        val useKingTheme = isKing || isEmpate

        holder.bind(player, useKingTheme, position + 1, isEmpate)
    }

    class ViewHolder(private val binding: ItemPlayerRodadaRankingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(player: PlayerState, useKingTheme: Boolean, rank: Int, isEmpate: Boolean) {
            if (useKingTheme) {
                binding.themeKing.isVisible = true
                binding.themeNormal.isVisible = false
                
                binding.textPlayerNameKing.text = player.playerName
                binding.textPlayerWinsKing.text = "${player.totalScore} Pontos"
                
                // Se estiver empatado, usa o ícone de empate. Caso contrário, é o Rei isolado.
                if (isEmpate) {
                    binding.buttonDetailsKing.setImageResource(R.drawable.ic_empate)
                    binding.buttonDetailsKing.rotation = 0f
                } else {
                    binding.buttonDetailsKing.setImageResource(R.drawable.ic_crown)
                    binding.buttonDetailsKing.rotation = -45f
                }
                
                if (!player.playerImage.isNullOrEmpty()) {
                    binding.imagePlayerAvatarKing.isVisible = true
                    binding.siglaNomeKing.isVisible = false
                    binding.imagePlayerAvatarKing.load(Uri.parse(player.playerImage))
                } else {
                    binding.imagePlayerAvatarKing.isVisible = false
                    binding.siglaNomeKing.isVisible = true
                    binding.siglaNomeKing.text = player.playerName.take(2).uppercase()
                }
            } else {
                binding.themeKing.isVisible = false
                binding.themeNormal.isVisible = true
                
                binding.textPlayerName.text = player.playerName
                binding.textPlayerWins.text = "${player.totalScore} Pontos"
                
                val iconRes = when (rank) {
                    2 -> R.drawable.ic_2
                    3 -> R.drawable.ic_3
                    4 -> R.drawable.ic_4
                    5 -> R.drawable.ic_5
                    6 -> R.drawable.ic_6
                    7 -> R.drawable.ic_7
                    8 -> R.drawable.ic_8
                    9 -> R.drawable.ic_9
                    else -> R.drawable.ic_person
                }
                binding.buttonDetails.setImageResource(iconRes)

                if (!player.playerImage.isNullOrEmpty()) {
                    binding.imagePlayerAvatar.isVisible = true
                    binding.siglaNome.isVisible = false
                    binding.imagePlayerAvatar.load(Uri.parse(player.playerImage))
                } else {
                    binding.imagePlayerAvatar.isVisible = false
                    binding.siglaNome.isVisible = true
                    binding.siglaNome.text = player.playerName.take(2).uppercase()
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PlayerState>() {
        override fun areItemsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem.playerName == newItem.playerName
        override fun areContentsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem == newItem
    }
}
