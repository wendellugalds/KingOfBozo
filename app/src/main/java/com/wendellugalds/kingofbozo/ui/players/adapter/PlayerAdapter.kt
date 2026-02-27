package com.wendellugalds.kingofbozo.ui.players.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.databinding.ItemPlayerGamersBinding
import com.wendellugalds.kingofbozo.model.Player

class PlayerAdapter(
    private val clickListener: (Player) -> Unit,
    private val selectionListener: () -> Unit,
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
                    if (!isSelectionMode && itemCount >= 1) {
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
            val context = binding.root.context
            binding.textPlayerName.text = player.name

            // Cores baseadas no tema
            val colorPrimary = context.getColorFromAttr(com.google.android.material.R.attr.colorPrimary)
            val textAppearanceButton = context.getColorFromAttr(com.google.android.material.R.attr.textAppearanceButton)
            val cardForegroundColor = context.getColorFromAttr(com.google.android.material.R.attr.cardBackgroundColor)
            val corfixa = Color.parseColor("#FFFFFF")





            if (isSelected) {
                // Tema Selecionado (Fundo Escuro/Verde Neon)
                binding.rootLayout.setBackgroundResource(R.drawable.item_background_selector)
                binding.rootLayout.backgroundTintList = ColorStateList.valueOf(colorPrimary)
                binding.textPlayerName.setTextColor(corfixa)
                binding.textPlayerWins.setTextColor(corfixa)
                TextViewCompat.setCompoundDrawableTintList(binding.textPlayerWins, ColorStateList.valueOf(corfixa))
                binding.textPlayerWins.alpha = 0.7f
                binding.siglaNome.backgroundTintList = ColorStateList.valueOf(corfixa)
                binding.siglaNome.setTextColor(colorPrimary)
                binding.checkboxSelectPlayer.buttonTintList = ColorStateList.valueOf(corfixa)
            } else {
                // Tema Não Selecionado (Inverso ou Padrão)
                binding.rootLayout.setBackgroundResource(R.drawable.item_background_selector)
                binding.rootLayout.backgroundTintList = ColorStateList.valueOf(cardForegroundColor)
                binding.textPlayerName.setTextColor(textAppearanceButton)
                binding.textPlayerWins.setTextColor(textAppearanceButton)
                TextViewCompat.setCompoundDrawableTintList(binding.textPlayerWins, ColorStateList.valueOf(colorPrimary))
                binding.textPlayerWins.alpha = 0.7f
                binding.siglaNome.backgroundTintList = ColorStateList.valueOf(colorPrimary)
                binding.siglaNome.setTextColor(corfixa)
                binding.checkboxSelectPlayer.buttonTintList = ColorStateList.valueOf(colorPrimary)
            }

            if (!player.imageUri.isNullOrEmpty()) {
                binding.imagePlayerAvatar.load(Uri.parse(player.imageUri)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_person)
                    transformations(CircleCropTransformation())
                }
                binding.imagePlayerAvatar.visibility = View.VISIBLE
                binding.siglaNome.visibility = View.GONE
            } else {
                binding.imagePlayerAvatar.visibility = View.GONE
                binding.siglaNome.visibility = View.VISIBLE
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

            if (isSelectionMode) {
                binding.checkboxSelectPlayer.visibility = View.VISIBLE
                binding.buttonDetails.visibility = View.GONE
                binding.checkboxSelectPlayer.isChecked = isSelected
            } else {
                binding.checkboxSelectPlayer.visibility = View.GONE
                binding.buttonDetails.visibility = View.VISIBLE
            }

            binding.textPlayerWins.text = if (player.wins == 1) "1 Vitória" else "${player.wins} Vitórias"
        }

        private fun android.content.Context.getColorFromAttr(@AttrRes attr: Int): Int {
            val typedValue = TypedValue()
            theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }
    }

    fun startSelectionMode() {
        if (!isSelectionMode && itemCount >= 1) {
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
        selectedItems.clear()
        selectedItems.addAll(currentList)
        notifyItemRangeChanged(0, itemCount)
        selectionListener()
    }

    fun deselectAll() {
        selectedItems.clear()
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
