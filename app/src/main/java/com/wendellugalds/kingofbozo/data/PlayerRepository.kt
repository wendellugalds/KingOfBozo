package com.wendellugalds.kingofbozo.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wendellugalds.kingofbozo.database.PlayerDao
import com.wendellugalds.kingofbozo.database.SavedGameDao
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.model.SavedGame
import kotlinx.coroutines.flow.Flow

class PlayerRepository(
    private val playerDao: PlayerDao,
    private val savedGameDao: SavedGameDao,
    private val context: Context
) {

    val allPlayers: Flow<List<Player>> = playerDao.getAlphabetizedPlayers()
    val allSavedGames: Flow<List<SavedGame>> = savedGameDao.getAllSavedGames()

    fun getPlayerById(playerId: Long): Flow<Player> {
        return playerDao.getPlayerById(playerId)
    }

    fun getPlayersByIds(playerIds: List<Long>): Flow<List<Player>> {
        return playerDao.getPlayersByIds(playerIds)
    }

    suspend fun insert(player: Player) {
        playerDao.insert(player)
    }

    suspend fun update(player: Player) {
        playerDao.update(player)
    }

    suspend fun resetAllPlayerStats() {
        playerDao.resetAllPlayerStats()
    }

    suspend fun deletePlayers(players: List<Player>) {
        val playerNames = players.map { it.name }
        val gson = Gson()
        val listType = object : TypeToken<List<PlayerState>>() {}.type

        // 1. Limpar permissões de URI de imagem (se houver) para remover resquícios do sistema
        players.forEach { player ->
            player.imageUri?.let { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    context.contentResolver.releasePersistableUriPermission(
                        uri, 
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignora se não houver permissão persistida
                }
            }
        }

        // 2. Buscar todos os jogos salvos para verificar a presença dos jogadores deletados
        val savedGames = savedGameDao.getAllSavedGamesList()

        for (game in savedGames) {
            val playerStates: List<PlayerState> = gson.fromJson(game.playerStatesJson, listType)
            
            // Filtrar a lista de jogadores do jogo, removendo os que estão sendo deletados do app
            val updatedPlayerStates = playerStates.filter { it.playerName !in playerNames }

            if (updatedPlayerStates.size <= 1) {
                // 3. Se restar 1 ou 0 jogadores, o jogo deve ser excluído (conforme sua solicitação)
                savedGameDao.delete(game)
            } else if (updatedPlayerStates.size < playerStates.size) {
                // 4. Se alguns jogadores foram removidos mas ainda restam pelo menos 2, atualiza o jogo
                val updatedJson = gson.toJson(updatedPlayerStates)
                
                var newIndex = game.currentPlayerIndex
                if (newIndex >= updatedPlayerStates.size) {
                    newIndex = 0
                }
                
                val updatedGame = game.copy(
                    playerStatesJson = updatedJson,
                    currentPlayerIndex = newIndex
                )
                savedGameDao.update(updatedGame)
            }
        }

        // 5. Por fim, excluir o(s) jogador(es) da tabela principal
        playerDao.deletePlayers(players)
    }

    suspend fun saveGame(savedGame: SavedGame): Long {
        return savedGameDao.insert(savedGame)
    }

    suspend fun deleteSavedGame(savedGame: SavedGame) {
        savedGameDao.delete(savedGame)
    }
}