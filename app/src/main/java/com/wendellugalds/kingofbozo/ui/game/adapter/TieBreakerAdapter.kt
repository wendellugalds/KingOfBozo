package com.wendellugalds.kingofbozo.ui.game.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.wendellugalds.kingofbozo.databinding.ItemTieBreakerBinding
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.R

class TieBreakerAdapter(
    private val tempValues: Map<Long, Int> = emptyMap(),
    private val onPlayerClick: (PlayerState) -> Unit
) : ListAdapter<PlayerState, TieBreakerAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTieBreakerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTieBreakerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(player: PlayerState) {
            binding.textPlayerName.text = player.playerName
            
            val hasAnyValueSelected = tempValues.values.any { it > 0 }
            val extra = tempValues[player.playerId] ?: 0
            
            val isDeactivated = hasAnyValueSelected && extra == 0
            
            if (extra > 0) {
                binding.textPlayerScore.text = "${player.totalScore} + $extra pts"
                binding.btnAction.alpha = 1.0f
                binding.btnAction.visibility = View.VISIBLE
            } else {
                binding.textPlayerScore.text = "${player.totalScore} pontos"
                binding.btnAction.alpha = 0.3f
                binding.btnAction.visibility = if (isDeactivated) View.INVISIBLE else View.VISIBLE
            }
            
            itemView.alpha = if (isDeactivated) 0.3f else 1.0f
            itemView.isEnabled = !isDeactivated
            binding.btnAction.isEnabled = !isDeactivated
            
            if (!player.playerImage.isNullOrEmpty()) {
                binding.imagePlayerAvatar.load(Uri.parse(player.playerImage)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_person)
                    transformations(CircleCropTransformation())
                }
                binding.imagePlayerAvatar.visibility = View.VISIBLE
                binding.siglaNome.visibility = View.GONE
            } else {
                binding.imagePlayerAvatar.visibility = View.GONE
                binding.siglaNome.visibility = View.VISIBLE
                val name = player.playerName.trim()
                val words = name.split(" ").filter { it.isNotBlank() }
                binding.siglaNome.text = if (words.size > 1) {
                    "${words.first().first()}${words.last().first()}"
                } else if (words.isNotEmpty()) {
                    words.first().take(2).uppercase()
                } else {
                    "--"
                }
            }

            binding.root.setOnClickListener { if (!isDeactivated) onPlayerClick(player) }
            binding.btnAction.setOnClickListener { if (!isDeactivated) onPlayerClick(player) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PlayerState>() {
        override fun areItemsTheSame(oldItem: PlayerState, newItem: PlayerState): Boolean =
            oldItem.playerId == newItem.playerId

        override fun areContentsTheSame(oldItem: PlayerState, newItem: PlayerState): Boolean =
            oldItem == newItem
    }
}
