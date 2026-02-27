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
import com.wendellugalds.kingofbozo.databinding.ItemPlayerRodadaRankingGeralBinding
import com.wendellugalds.kingofbozo.model.PlayerState

class RankingGeralAdapter : ListAdapter<PlayerState, RankingGeralAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPlayerRodadaRankingGeralBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val player = getItem(position)
        holder.bind(player, position == 0, position + 1)
    }

    class ViewHolder(private val binding: ItemPlayerRodadaRankingGeralBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(player: PlayerState, isFirst: Boolean, rank: Int) {
            if (isFirst) {
                binding.themeKingRakingGeral.isVisible = true
                binding.themeRakingGeral.isVisible = false
                
                binding.textPlayerNameKing.text = player.playerName
                binding.textWinsKing.text = "${player.sessionWins} Vitórias"
                binding.textScoreWinsKing.text = "${player.sessionTotalPoints} Pontos"
                
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
                binding.themeKingRakingGeral.isVisible = false
                binding.themeRakingGeral.isVisible = true
                
                binding.textPlayerName.text = player.playerName
                binding.textWins.text = "${player.sessionWins} Vitórias"
                binding.textScoreWins.text = "${player.sessionTotalPoints} Pontos"
                
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
                binding.textPlayerName.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)

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
