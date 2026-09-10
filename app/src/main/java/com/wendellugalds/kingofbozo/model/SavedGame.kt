package com.wendellugalds.kingofbozo.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

@Keep
@Entity(tableName = "saved_games")
data class SavedGame(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id") val id: Long = 0,
    @SerializedName("date") val date: Long = System.currentTimeMillis(),
    @SerializedName("currentRound") val currentRound: Int,
    @SerializedName("playerStatesJson") val playerStatesJson: String, // Armazena a lista de PlayerState como JSON
    @SerializedName("accumulatedTimeMillis") val accumulatedTimeMillis: Long = 0,
    @SerializedName("currentPlayerIndex") val currentPlayerIndex: Int = 0,
    @SerializedName("status") val status: GameStatus = GameStatus.ONGOING,
    @SerializedName("tiedPlayerIdsJson") val tiedPlayerIdsJson: String? = null // IDs dos jogadores que aguardam desempate como JSON
)

@Keep
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

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Long>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromGameStatus(status: GameStatus): String {
        return status.name
    }

    @TypeConverter
    fun toGameStatus(name: String): GameStatus {
        return try {
            GameStatus.valueOf(name)
        } catch (_: Exception) {
            GameStatus.ONGOING
        }
    }
}
