package com.wendellugalds.kingofbozo.model

import androidx.annotation.Keep

@Keep
enum class CategoryType {
    AS, DUQUE, TERNO, QUADRA, QUINA, SENA, FULL, SEGUIDA, QUADRADA, GENERAL
}

@Keep
enum class GameStatus {
    ONGOING,
    WAITING_TIE_BREAKER,
    FINISHED
}

@Keep
data class PlayerScore(
    val name: String,
    var totalScore: Int
)