package com.wendellugalds.kingofbozo.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Representa o estado de um único jogador em um determinado momento.
 */
@Keep
data class PlayerState(
    @SerializedName("playerId") val playerId: Long = 0,
    @SerializedName("playerName") val playerName: String,
    @SerializedName("playerImage") val playerImage: String? = null,
    @SerializedName("scores") val scores: MutableMap<CategoryType, ScoreEntry?>,
    @SerializedName("totalScore") var totalScore: Int = 0,
    @SerializedName("sessionWins") var sessionWins: Int = 0,
    @SerializedName("sessionTotalPoints") var sessionTotalPoints: Int = 0,
    @SerializedName("totalWins") var totalWins: Int = 0,
    @SerializedName("totalPoints") var totalPoints: Int = 0
)

/**
 * Representa o estado completo do jogo em um determinado momento.
 */
@Keep
data class GameState(
    @SerializedName("gameId") val gameId: Long = 0,
    @SerializedName("playersState") val playersState: List<PlayerState>,
    @SerializedName("currentPlayerIndex") val currentPlayerIndex: Int,
    @SerializedName("currentRound") val currentRound: Int,
    @SerializedName("startTimeMillis") val startTimeMillis: Long = System.currentTimeMillis(),
    @SerializedName("accumulatedTimeMillis") val accumulatedTimeMillis: Long = 0,
    @SerializedName("status") val status: GameStatus = GameStatus.ONGOING,
    @SerializedName("tiedPlayerIds") val tiedPlayerIds: List<Long>? = null
)
