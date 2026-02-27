package com.wendellugalds.kingofbozo.model

/**
 * Representa o estado de um único jogador em um determinado momento.
 */
data class PlayerState(
    val playerName: String,
    val playerImage: String? = null,
    val scores: MutableMap<CategoryType, ScoreEntry?>,
    var totalScore: Int = 0,
    var sessionWins: Int = 0,
    var sessionTotalPoints: Int = 0,
    var totalWins: Int = 0,
    var totalPoints: Int = 0
)

/**
 * Representa o estado completo do jogo em um determinado momento.
 */
data class GameState(
    val gameId: Long = 0,
    val playersState: List<PlayerState>,
    val currentPlayerIndex: Int,
    val currentRound: Int,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val accumulatedTimeMillis: Long = 0
)
