package com.wendellugalds.kingofbozo.database

import androidx.room.*
import com.wendellugalds.kingofbozo.model.SavedGame
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_games ORDER BY date DESC")
    fun getAllSavedGames(): Flow<List<SavedGame>>

    @Query("SELECT * FROM saved_games")
    suspend fun getAllSavedGamesList(): List<SavedGame>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savedGame: SavedGame): Long

    @Delete
    suspend fun delete(savedGame: SavedGame)

    @Update
    suspend fun update(savedGame: SavedGame)
}
