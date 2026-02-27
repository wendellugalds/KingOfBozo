package com.wendellugalds.kingofbozo.ui.game.adapter

import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ItemJogadorSelectionBinding
import com.wendellugalds.kingofbozo.model.PlayerState

class PlayerMarkerAdapter(
    private val onPlayerClick: (Int) -> Unit
) : ListAdapter<PlayerState, PlayerMarkerAdapter.ViewHolder>(DiffCallback()) {

    private var currentPlayerIndex: Int = -1
    private var maxScore: Int = -1
    private var winnersCount: Int = 0
    private var scoreCounts: Map<Int, Int> = emptyMap()

    fun updateState(index: Int, players: List<PlayerState>) {
        val oldIndex = currentPlayerIndex
        currentPlayerIndex = index
        maxScore = players.maxOfOrNull { it.totalScore } ?: -1
        winnersCount = players.count { it.totalScore == maxScore && it.totalScore > 0 }
        
        scoreCounts = players.filter { it.totalScore > 0 }
            .groupingBy { it.totalScore }
            .eachCount()
            
        submitList(players.toList())
        
        // Notificamos todos para garantir que a animação de subida e descida aconteça
        // O ListAdapter às vezes não notifica itens se o conteúdo do objeto PlayerState for idêntico
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemJogadorSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playerState = getItem(position)
        holder.bind(playerState, position == currentPlayerIndex, position)
    }

    inner class ViewHolder(private val binding: ItemJogadorSelectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playerState: PlayerState, isSelected: Boolean, position: Int) {
            val context = binding.root.context
            
            // Imagem ou Sigla
            if (!playerState.playerImage.isNullOrEmpty()) {
                binding.imagePlayerAvatarJogo.visibility = View.VISIBLE
                binding.imagePlayerAvatarJogo.load(Uri.parse(playerState.playerImage))
                binding.siglaNome.visibility = View.GONE
            } else {
                binding.imagePlayerAvatarJogo.visibility = View.GONE
                binding.siglaNome.visibility = View.VISIBLE
                binding.siglaNome.text = playerState.playerName.take(2).uppercase()
            }
            
            // Animação de subida (Destaque)
            val riseAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -30f, context.resources.displayMetrics)
            
            binding.root.animate().cancel()
            if (isSelected) {
                binding.root.translationY = 0f // Reset para garantir início limpo
                binding.root.animate()
                    .translationY(riseAmount)
                    .setDuration(1000)
                    .setInterpolator(OvershootInterpolator(1.5f))
                    .alpha(1.0f)
                    .start()
            } else {
                binding.root.animate()
                    .translationY(0f)
                    .setDuration(1000)
                    .alpha(0.6f)
                    .start()
            }
            
            // Ordem do jogador
            val positionIcon = when (position) {
                0 -> R.drawable.ic_1
                1 -> R.drawable.ic_2
                2 -> R.drawable.ic_3
                3 -> R.drawable.ic_4
                4 -> R.drawable.ic_5
                5 -> R.drawable.ic_6
                6 -> R.drawable.ic_7
                7 -> R.drawable.ic_8
                8 -> R.drawable.ic_9
                else -> R.drawable.ic_0
            }
            binding.iconPosition.setImageResource(positionIcon)

            // Lógica de Coroa (Rei) e Bug (Empate)
            val score = playerState.totalScore
            val isSoleWinner = score == maxScore && winnersCount == 1 && score > 0
            val countWithSameScore = scoreCounts[score] ?: 0
            val isTied = countWithSameScore > 1

            binding.iconKing.isVisible = isSoleWinner
            binding.iconEmpate.isVisible = isTied && !isSoleWinner

            binding.root.setOnClickListener {
                onPlayerClick(position)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PlayerState>() {
        override fun areItemsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem.playerName == newItem.playerName
        override fun areContentsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem == newItem
    }
}
