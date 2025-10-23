package com.wendellugalds.kingofbozo.ui.game.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ItemPlayerMarcadorBinding
import com.wendellugalds.kingofbozo.model.GameState
import com.wendellugalds.kingofbozo.model.Player

class PlayerMarkerAdapter : ListAdapter<Player, PlayerMarkerAdapter.PlayerMarkerViewHolder>(DiffCallback()) {

    private var currentPlayerIndex = 0
    private var gameState: GameState? = null

    fun updateGameState(newState: GameState) {
        gameState = newState
        notifyItemRangeChanged(0, itemCount)
    }

    fun setCurrentPlayerIndex(index: Int) {
        val oldIndex = currentPlayerIndex
        if (oldIndex != index) {
            currentPlayerIndex = index
            // Notifica o RecyclerView para redesenhar o item que perdeu o foco e o que ganhou.
            notifyItemChanged(oldIndex)
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerMarkerViewHolder {
        val binding =
            ItemPlayerMarcadorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlayerMarkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerMarkerViewHolder, position: Int) {
        holder.bind(getItem(position), position == currentPlayerIndex)
    }

    inner class PlayerMarkerViewHolder(private val binding: ItemPlayerMarcadorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context: Context = binding.root.context

        fun Int.dpToPx(): Int = (this * context.resources.displayMetrics.density).toInt()

        fun bind(player: Player, isCurrentPlayer: Boolean) {
            val playerState = gameState?.playersState?.find { it.playerName == player.name }
            binding.playerScore.text = playerState?.totalScore?.toString() ?: "0"
            binding.playerName.text = player.name

            // A lógica de aparência é definida aqui, baseada no isCurrentPlayer
            if (isCurrentPlayer) {
                binding.cardContainer.setBackgroundResource(R.drawable.background_card_2_destaque)
                binding.playerScore.setTextColor(ContextCompat.getColor(context, R.color.black))
                binding.playerName.setTextColor(ContextCompat.getColor(context, R.color.black))
                binding.playerName.textSize = 17f
                binding.playerScore.textSize = 55f

                val params = binding.avatarContainer.layoutParams
                params.width = 50.dpToPx()
                params.height = 50.dpToPx()
                binding.avatarContainer.layoutParams = params
            } else {
                binding.cardContainer.setBackgroundResource(R.drawable.background_card_2_dark)
                binding.playerScore.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.playerName.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.playerName.textSize = 12f
                binding.playerScore.textSize = 35f

                val params = binding.avatarContainer.layoutParams
                params.width = 50.dpToPx()
                params.height = 50.dpToPx()
                binding.avatarContainer.layoutParams = params

                //binding.cardContainer.setBackgroundResource(R.drawable.background_transparente)
                //binding.playerScore.setTextColor(ContextCompat.getColor(context, R.color.transparente))
                //binding.playerName.setTextColor(ContextCompat.getColor(context, R.color.transparente))
                // Você pode resetar os tamanhos se desejar, mas a escala já cuida disso visualmente.

                //val params = binding.avatarContainer.layoutParams
                //params.width = 120.dpToPx()
                //params.height = 120.dpToPx()
                //binding.avatarContainer.layoutParams = params
            }

            // Lógica da imagem/sigla
            if (!player.imageUri.isNullOrEmpty()) {
                binding.imagePlayerPlaceholder.load(Uri.parse(player.imageUri)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_person)
                    error(R.drawable.ic_person)
                    transformations(CircleCropTransformation())
                }
                binding.imagePlayerPlaceholder.visibility = View.VISIBLE
                binding.siglaNome.visibility = View.GONE
            } else {
                binding.imagePlayerPlaceholder.visibility = View.GONE
                binding.siglaNome.visibility = View.VISIBLE
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
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Player>() {
        override fun areItemsTheSame(oldItem: Player, newItem: Player) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Player, newItem: Player) = oldItem == newItem
    }
}