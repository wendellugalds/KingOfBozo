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

        // --- AJUSTE: Listeners movidos para o init ---
        // Isso melhora a performance, pois os listeners são criados apenas uma vez por ViewHolder,
        // em vez de serem recriados toda vez que o método bind() é chamado.
        init {
            itemView.setOnClickListener {
                // Previne cliques durante animações de remoção
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
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        // --- AJUSTE: Substituído notifyDataSetChanged() para melhor performance ---
                        notifyItemRangeChanged(0, itemCount)
                        toggleSelection(getItem(bindingAdapterPosition))
                    }
                    true // Consome o evento de long click
                } else {
                    false
                }
            }
        }

        fun bind(player: Player) {
            val isSelected = selectedItems.contains(player)
            binding.textPlayerName.text = player.name

            // --- AJUSTE: Uso de "plurals" para tratar singular/plural e facilitar tradução ---
            // Adicione o seguinte em seu arquivo res/values/strings.xml:
            // <plurals name="number_of_wins">
            //     <item quantity="one">%d Vitória</item>
            //     <item quantity="other">%d Vitórias</item>
            // </plurals>
           // val context = binding.root.context
            //binding.textPlayerWins.text = context.resources.getQuantityString(
            //    R.plurals.number_of_wins, player.wins, player.wins
            //)

            if (!player.imageUri.isNullOrEmpty()) {
                binding.imagePlayerAvatar.load(Uri.parse(player.imageUri))
                binding.imagePlayerAvatar.visibility = View.VISIBLE



            } else {
                val name = player.name?.trim() ?: ""

// Divide o nome em palavras, ignorando espaços extras entre elas
                val words = name.split(" ").filter { it.isNotBlank() }

                val initials = if (words.size > 1) {
                    // REGRA 1: Se tem mais de uma palavra, pega a primeira letra da primeira e da última palavra.
                    // Ex: "Wendell Ugalds" -> "WU"
                    // Ex: "Selma Regina da Silva" -> "SS"
                    val firstInitial = words.first().first()
                    val lastInitial = words.last().first()
                    binding.siglaNome.text = "$firstInitial$lastInitial"
                } else if (words.isNotEmpty()) {
                    // REGRA 2: Se tem apenas uma palavra...
                    val word = words.first()
                    if (word.length >= 2) {
                        // Pega as duas primeiras letras
                        // Ex: "Lorena" -> "LO"
                        binding.siglaNome.text = word.substring(0, 2)
                    } else {
                        // Se a palavra tiver só uma letra, pega apenas ela
                        binding.siglaNome.text = word
                    }
                } else {
                    // Caso o nome esteja vazio, retorna "--" ou uma string vazia
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
        // --- AJUSTE: Substituído notifyDataSetChanged() para melhor performance ---
        notifyItemRangeChanged(0, itemCount)
    }

    fun selectAll() {
        if (selectedItems.size == itemCount) {
            selectedItems.clear()
        } else {
            selectedItems.clear()
            selectedItems.addAll(currentList)
        }
        // --- AJUSTE: Substituído notifyDataSetChanged() para melhor performance ---
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