package com.wendellugalds.kingofbozo.data

import com.wendellugalds.kingofbozo.database.PlayerDao
import com.wendellugalds.kingofbozo.model.Player
import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {

    val allPlayers: Flow<List<Player>> = playerDao.getAlphabetizedPlayers()

    fun getPlayerById(playerId: Long): Flow<Player> {
        return playerDao.getPlayerById(playerId)
    }

    // --- FUNÇÃO ADICIONADA ---
    fun getPlayersByIds(playerIds: List<Long>): Flow<List<Player>> {
        return playerDao.getPlayersByIds(playerIds)
    }

    suspend fun insert(player: Player) {
        playerDao.insert(player)
    }

    suspend fun update(player: Player) {
        playerDao.update(player)
    }

    suspend fun deletePlayers(players: List<Player>) {
        playerDao.deletePlayers(players)
    }
}