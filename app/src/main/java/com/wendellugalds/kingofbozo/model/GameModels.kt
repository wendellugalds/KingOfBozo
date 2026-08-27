package com.wendellugalds.kingofbozo.model

enum class CategoryType {
    AS, DUQUE, TERNO, QUADRA, QUINA, SENA, FULL, SEGUIDA, QUADRADA, GENERAL
}

enum class GameStatus {
    ONGOING,
    WAITING_TIE_BREAKER,
    FINISHED
}

data class PlayerScore(
    val name: String,
    var totalScore: Int
)