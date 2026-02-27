package com.wendellugalds.kingofbozo.ui.game.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.graphics.ColorUtils
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
            val context = binding.root.context
            binding.textPlayerName.text = player.name

            // Cores baseadas no tema
            val colorPrimary = context.getColorFromAttr(com.google.android.material.R.attr.colorPrimary)
            val corfixa = Color.parseColor("#FFFFFF")

            if (item.isSelected) {
                // Tema Selecionado (Fundo Escuro/Verde Neon)
                binding.rootLayout.setBackgroundResource(R.drawable.item_background_selector)
                binding.rootLayout.backgroundTintList = ColorStateList.valueOf(corfixa)
                binding.textPlayerName.setTextColor(colorPrimary)
                binding.siglaNome.backgroundTintList = ColorStateList.valueOf(colorPrimary)
                binding.siglaNome.setTextColor(corfixa)
                binding.checkboxSelectPlayer.buttonTintList = ColorStateList.valueOf(colorPrimary)
                binding.imageSelectionIconBadge.imageTintList = ColorStateList.valueOf(colorPrimary)
            } else {
                // Tema Não Selecionado (Inverso ou Padrão)
                binding.rootLayout.setBackgroundResource(R.drawable.item_background_selector)
                binding.rootLayout.backgroundTintList = ColorStateList.valueOf(
                    ColorUtils.setAlphaComponent(corfixa, 25) // 15% Alpha
                )
                binding.textPlayerName.setTextColor(corfixa)
                binding.siglaNome.backgroundTintList = ColorStateList.valueOf(corfixa)
                binding.siglaNome.setTextColor(colorPrimary)
                binding.checkboxSelectPlayer.buttonTintList = ColorStateList.valueOf(corfixa)
                binding.imageSelectionIconBadge.imageTintList = ColorStateList.valueOf(corfixa)
            }

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
                binding.siglaNome.text = if (words.size > 1) {
                    "${words.first().first()}${words.last().first()}"
                } else if (words.isNotEmpty()) {
                    words.first().take(2).uppercase()
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

        private fun android.content.Context.getColorFromAttr(@AttrRes attr: Int): Int {
            val typedValue = TypedValue()
            theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
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
