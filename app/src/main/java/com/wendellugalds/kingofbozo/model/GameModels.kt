package com.wendellugalds.kingofbozo.model

enum class CategoryType {
    AS, DUQUE, TERNO, QUADRA, QUINA, SENA, FULL, SEGUIDA, QUADRADA, GENERAL
}

data class PlayerScore(
    val name: String,
    var totalScore: Int
)