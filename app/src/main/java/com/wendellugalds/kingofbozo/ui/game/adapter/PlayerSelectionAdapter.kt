package com.wendellugalds.kingofbozo.ui.game.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ItemPlayerSelectionBinding
import com.wendellugalds.kingofbozo.model.Player

data class SelectablePlayerItem(
    val player: Player,
    val isSelected: Boolean,
    val selectionOrder: Int?
)

class PlayerSelectionAdapter(
    private val onPlayerClick: (Player) -> Unit
) : ListAdapter<SelectablePlayerItem, PlayerSelectionAdapter.PlayerViewHolder>(PlayerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlayerViewHolder(private val binding: ItemPlayerSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onPlayerClick(getItem(bindingAdapterPosition).player)
                }
            }
        }

        fun bind(item: SelectablePlayerItem) {
            val player = item.player
            binding.textPlayerName.text = player.name

            if (!player.imageUri.isNullOrEmpty()) {
                binding.imagePlayerAvatar.load(Uri.parse(player.imageUri)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_person)
                    transformations(CircleCropTransformation())
                }
                binding.imagePlayerAvatar.visibility = View.VISIBLE
            } else {
                binding.imagePlayerAvatar.visibility = View.GONE

                val name = player.name?.trim() ?: ""

                val words = name.split(" ").filter { it.isNotBlank() }

                val initials = if (words.size > 1) {
                    val firstInitial = words.first().first()
                    val lastInitial = words.last().first()
                    binding.siglaNome.text = "$firstInitial$lastInitial"
                } else if (words.isNotEmpty()) {
                    val word = words.first()
                    if (word.length >= 2) {
                        binding.siglaNome.text = word.substring(0, 2)
                    } else {
                        binding.siglaNome.text = word
                    }
                } else {
                    "--"
                }
            }

            binding.checkboxSelectPlayer.isChecked = item.isSelected

            binding.imageSelectionIconBadge.isVisible = item.isSelected
            if (item.isSelected) {
                val badgeDrawable = when (item.selectionOrder) {
                    1 -> R.drawable.ic_1
                    2 -> R.drawable.ic_2
                    3 -> R.drawable.ic_3
                    4 -> R.drawable.ic_4
                    5 -> R.drawable.ic_5
                    6 -> R.drawable.ic_6
                    7 -> R.drawable.ic_7
                    8 -> R.drawable.ic_8
                    9 -> R.drawable.ic_9
                    else -> 0
                }
                if (badgeDrawable != 0) {
                    binding.imageSelectionIconBadge.setImageResource(badgeDrawable)
                } else {
                    binding.imageSelectionIconBadge.setImageDrawable(null)
                }
            }
        }
    }
}

class PlayerDiffCallback : DiffUtil.ItemCallback<SelectablePlayerItem>() {
    override fun areItemsTheSame(oldItem: SelectablePlayerItem, newItem: SelectablePlayerItem): Boolean {
        return oldItem.player.id == newItem.player.id
    }

    override fun areContentsTheSame(oldItem: SelectablePlayerItem, newItem: SelectablePlayerItem): Boolean {
        return oldItem == newItem
    }
}
