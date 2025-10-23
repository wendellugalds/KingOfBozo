package com.wendellugalds.kingofbozo.ui.players

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wendellugalds.kingofbozo.data.PlayerRepository
import com.wendellugalds.kingofbozo.model.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: PlayerRepository) : ViewModel() {

    val players: LiveData<List<Player>> = repository.allPlayers.asLiveData()

    // --- FUNÇÃO ADICIONADA ---
    // A UI (PlayerDetailFragment) vai chamar esta função para obter os dados
    // de um jogador específico e observá-los.
    fun getPlayerById(playerId: Long): LiveData<Player> {
        return repository.getPlayerById(playerId).asLiveData()
    }

    fun addPlayer(player: Player) {
        viewModelScope.launch {
            repository.insert(player)
        }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            repository.update(player)
        }
    }

    fun deletePlayers(playersToDelete: List<Player>) {
        viewModelScope.launch {
            repository.deletePlayers(playersToDelete)
        }
    }
}