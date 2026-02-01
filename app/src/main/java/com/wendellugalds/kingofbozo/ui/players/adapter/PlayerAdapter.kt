package com.wendellugalds.kingofbozo.ui.players.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ItemPlayerGamersBinding
import com.wendellugalds.kingofbozo.model.Player

class PlayerAdapter(
    private val clickListener: (Player) -> Unit,
    private val selectionListener: () -> Unit
) : ListAdapter<Player, PlayerAdapter.PlayerViewHolder>(PlayerDiffCallback()) {

    var isSelectionMode = false
        private set

    private val selectedItems = mutableSetOf<Player>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerGamersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = getItem(position)
        holder.bind(player)
    }

    inner class PlayerViewHolder(private val binding: ItemPlayerGamersBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    val player = getItem(bindingAdapterPosition)
                    if (isSelectionMode) {
                        toggleSelection(player)
                    } else {
                        clickListener(player)
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    if (!isSelectionMode && itemCount > 1) {
                        startSelectionMode()
                        toggleSelection(getItem(bindingAdapterPosition))
                    }
                    true
                } else {
                    false
                }
            }
        }

        fun bind(player: Player) {
            val isSelected = selectedItems.contains(player)
            binding.textPlayerName.text = player.name

            if (!player.imageUri.isNullOrEmpty()) {
                binding.imagePlayerAvatar.load(Uri.parse(player.imageUri))
                binding.imagePlayerAvatar.visibility = View.VISIBLE
            } else {
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
                binding.imagePlayerAvatar.visibility = View.GONE
            }

            if (isSelectionMode) {
                binding.checkboxSelectPlayer.visibility = View.VISIBLE
                binding.buttonDetails.visibility = View.GONE
                binding.checkboxSelectPlayer.isChecked = isSelected
            } else {
                binding.checkboxSelectPlayer.visibility = View.GONE
                binding.buttonDetails.visibility = View.VISIBLE
            }
            itemView.isActivated = isSelected
        }
    }

    fun startSelectionMode() {
        if (!isSelectionMode && itemCount > 1) {
            isSelectionMode = true
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun toggleSelection(player: Player) {
        if (selectedItems.contains(player)) {
            selectedItems.remove(player)
        } else {
            selectedItems.add(player)
        }
        selectionListener()
        notifyItemChanged(currentList.indexOf(player))
    }

    fun getSelectedItems(): List<Player> {
        return selectedItems.toList()
    }

    fun finishSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        notifyItemRangeChanged(0, itemCount)
    }

    fun selectAll() {
        if (selectedItems.size == itemCount) {
            selectedItems.clear()
        } else {
            selectedItems.clear()
            selectedItems.addAll(currentList)
        }
        notifyItemRangeChanged(0, itemCount)
        selectionListener()
    }
}

class PlayerDiffCallback : DiffUtil.ItemCallback<Player>() {
    override fun areItemsTheSame(oldItem: Player, newItem: Player): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean {
        return oldItem == newItem
    }
}
