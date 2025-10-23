// GameModels.kt
package com.example.app.ui // Use o seu pacote

// Enum para identificar cada categoria de forma única e segura
enum class CategoryType {
    AZ, DUQUE, TERNO, QUADRA, QUINA, SENA, FULL, SEGUIDA, QUADRADA, GENERAL
}

// Representa uma categoria na grade
data class Category(
    val type: CategoryType,
    val name: String,
    var score: Int? = null // Inicia como nulo, será preenchido quando o jogador pontuar
)

// Representa o estado de um jogador no placar
data class PlayerScore(
    val name: String,
    var totalScore: Int
)