package com.wendellugalds.kingofbozo.ui.savedgames

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.model.PlayerState

class SavedGamePlayersAdapter(private val currentRound: Int) : ListAdapter<PlayerState, SavedGamePlayersAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_game_player_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val player = getItem(position)
        
        val maxRoundScore = currentList.maxOfOrNull { it.totalScore } ?: 0
        val winnersCount = currentList.count { it.totalScore == maxRoundScore }
        
        val countWithSameScore = currentList.count { it.totalScore == player.totalScore }
        val isEmpate = countWithSameScore > 1
        
        val isKing = player.totalScore == maxRoundScore && winnersCount == 1 && player.totalScore > 0
        val isEmpateNoTopo = isEmpate && player.totalScore == maxRoundScore && player.totalScore > 0
        
        holder.bind(player, isKing, isEmpateNoTopo, position + 1, currentRound)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textPlayerName: TextView = itemView.findViewById(R.id.text_player_name)
        private val textPlayerScores: TextView = itemView.findViewById(R.id.text_player_scores)
        private val imagePlayerAvatar: ImageView = itemView.findViewById(R.id.image_player_avatar)
        private val siglaNome: TextView = itemView.findViewById(R.id.sigla_nome)
        private val iconRank: ImageView = itemView.findViewById(R.id.icon_rank)

        fun bind(player: PlayerState, isKing: Boolean, isEmpateNoTopo: Boolean, rank: Int, currentRound: Int) {
            textPlayerName.text = player.playerName
            textPlayerScores.text = "${player.totalScore} pontos"

            // Avatar
            if (!player.playerImage.isNullOrEmpty()) {
                imagePlayerAvatar.visibility = View.VISIBLE
                siglaNome.visibility = View.GONE
                imagePlayerAvatar.load(Uri.parse(player.playerImage)) {
                    transformations(CircleCropTransformation())
                }
            } else {
                imagePlayerAvatar.visibility = View.GONE
                siglaNome.visibility = View.VISIBLE
                siglaNome.text = player.playerName.take(2).uppercase()
            }

            // Rank Icon
            val iconRes = when {
                isKing -> R.drawable.ic_crown
                isEmpateNoTopo -> R.drawable.ic_bug_tie
                else -> {
                    when (rank) {
                        1 -> R.drawable.ic_1
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
                }
            }
            iconRank.setImageResource(iconRes)
            iconRank.rotation = if (isKing) -45f else 0f
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PlayerState>() {
        override fun areItemsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem.playerName == newItem.playerName
        override fun areContentsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem == newItem
    }
}
