package com.wendellugalds.kingofbozo.ui.game

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ItemManagePlayerBinding
import com.wendellugalds.kingofbozo.model.Player

class ManagePlayersAdapter(
    private val onAdd: ((Player) -> Unit)? = null,
    private val onRemove: ((String) -> Unit)? = null
) : ListAdapter<Player, ManagePlayersAdapter.ViewHolder>(DiffCallback) {

    private var isInGameList: Boolean = false

    fun submitList(list: List<Player>?, isInGame: Boolean) {
        val oldSize = currentList.size
        this.isInGameList = isInGame
        super.submitList(list) {
            val newSize = list?.size ?: 0
            if (isInGame && ((oldSize <= 2 && newSize > 2) || (oldSize > 2 && newSize <= 2))) {
                notifyItemRangeChanged(0, newSize)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemManagePlayerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val player = getItem(position)
        holder.bind(player, isInGameList)
    }

    inner class ViewHolder(private val binding: ItemManagePlayerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(player: Player, isInGame: Boolean) {
            binding.textPlayerName.text = player.name
            
            if (!player.imageUri.isNullOrEmpty()) {
                binding.imagePlayerAvatar.visibility = View.VISIBLE
                binding.imagePlayerAvatar.load(Uri.parse(player.imageUri))
                binding.siglaNome.visibility = View.GONE
            } else {
                binding.imagePlayerAvatar.visibility = View.GONE
                binding.siglaNome.visibility = View.VISIBLE
                
                val name = player.name.trim()
                val words = name.split(" ").filter { it.isNotBlank() }
                val initials = if (words.size > 1) {
                    "${words.first().first()}${words.last().first()}"
                } else if (words.isNotEmpty()) {
                    val word = words.first()
                    if (word.length >= 2) word.substring(0, 2) else word
                } else {
                    "--"
                }
                binding.siglaNome.text = initials.uppercase()
            }

            if (isInGame) {
                if (currentList.size <= 2) {
                    binding.btnAction.visibility = View.GONE
                    binding.root.setOnClickListener(null)
                    binding.root.isClickable = false
                } else {
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.setImageResource(R.drawable.ic_close)
                    binding.root.setOnClickListener { onRemove?.invoke(player.name) }
                    binding.root.isClickable = true
                }
            } else {
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.setImageResource(R.drawable.ic_switch_check)
                binding.root.setOnClickListener { onAdd?.invoke(player) }
                binding.root.isClickable = true
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Player>() {
        override fun areItemsTheSame(oldItem: Player, newItem: Player): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean {
            return oldItem == newItem
        }
    }
}