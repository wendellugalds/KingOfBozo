package com.wendellugalds.kingofbozo.model

/**
 * Representa o estado de um único jogador em um determinado momento.
 * @param playerName O nome do jogador.
 * @param scores Um mapa contendo a pontuação (e se foi risco) para cada categoria.
 * @param totalScore A soma de todas as pontuações do jogador.
 */
data class PlayerState(
    val playerName: String,
    // Alterado para usar o novo ScoreEntry
    val scores: MutableMap<CategoryType, ScoreEntry?>,
    var totalScore: Int = 0
)

/**
 * Representa o estado completo do jogo em um determinado momento.
 * @param playersState A lista com o estado de cada jogador.
 * @param currentPlayerIndex O índice do jogador atual na lista playersState.
 * @param currentRound A rodada atual do jogo (de 1 a 10).
 */
data class GameState(
    val playersState: List<PlayerState>,
    val currentPlayerIndex: Int,
    val currentRound: Int
)