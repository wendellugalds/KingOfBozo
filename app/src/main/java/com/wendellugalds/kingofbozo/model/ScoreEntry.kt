package com.wendellugalds.kingofbozo.model

// Armazena a pontuação e o tipo de jogada (normal ou riscada)
data class ScoreEntry(
    val value: Int,
    val isScratch: Boolean = false
)