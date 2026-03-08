package com.wendellugalds.kingofbozo.ui.savedgames

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wendellugalds.kingofbozo.databinding.ItemJogoSalvoBinding
import com.wendellugalds.kingofbozo.model.SavedGame
import com.wendellugalds.kingofbozo.model.PlayerState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SavedGamesAdapter(
    private val onClick: (SavedGame) -> Unit,
    private val onDelete: (SavedGame) -> Unit
) : ListAdapter<SavedGame, SavedGamesAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemJogoSalvoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemJogoSalvoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = getItem(position)
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(game.date))
        
        val gson = Gson()
        val listType = object : TypeToken<List<PlayerState>>() {}.type
        val playerStates: List<PlayerState> = gson.fromJson(game.playerStatesJson, listType)
        
        val totalPlayers = playerStates.size
        val totalPoints = playerStates.sumOf { it.sessionTotalPoints }
        
        val hours = TimeUnit.MILLISECONDS.toHours(game.accumulatedTimeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(game.accumulatedTimeMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(game.accumulatedTimeMillis) % 60
        val durationStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)


        holder.binding.savedGameTitle.text = "$dateStr"
        holder.binding.savedGameTime.text = "$durationStr"
        holder.binding.savedGamePlayers.text = "${totalPlayers}"
        holder.binding.savedGameRounds.text = "${game.currentRound}"
        holder.binding.savedGameTotalScore.text = "$totalPoints PONTOS"

        holder.itemView.setOnClickListener { onClick(game) }
        holder.binding.btnDelete.setOnClickListener { onDelete(game) }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SavedGame>() {
        override fun areItemsTheSame(oldItem: SavedGame, newItem: SavedGame) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SavedGame, newItem: SavedGame) = oldItem == newItem
    }
}
