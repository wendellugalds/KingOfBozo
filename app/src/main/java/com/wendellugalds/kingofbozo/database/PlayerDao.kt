package com.wendellugalds.kingofbozo.database

import androidx.room.*
import com.wendellugalds.kingofbozo.model.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAlphabetizedPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE id = :playerId")
    fun getPlayerById(playerId: Long): Flow<Player>

    // --- FUNÇÃO ADICIONADA ---
    @Query("SELECT * FROM players WHERE id IN (:playerIds)")
    fun getPlayersByIds(playerIds: List<Long>): Flow<List<Player>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(player: Player)

    @Update
    suspend fun update(player: Player)

    @Delete
    suspend fun delete(player: Player)

    @Delete
    suspend fun deletePlayers(players: List<Player>)
}