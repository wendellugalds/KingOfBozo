package com.wendellugalds.kingofbozo.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "saved_games")
data class SavedGame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val currentRound: Int,
    val playerStatesJson: String, // Armazena a lista de PlayerState como JSON
    val accumulatedTimeMillis: Long = 0,
    val currentPlayerIndex: Int = 0
)

class Converters {
    @TypeConverter
    fun fromPlayerStateList(value: List<PlayerState>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toPlayerStateList(value: String): List<PlayerState> {
        val listType = object : TypeToken<List<PlayerState>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
