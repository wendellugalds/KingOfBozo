package com.wendellugalds.kingofbozo.model

import java.io.Serializable

enum class CategoryType {
    AS,
    FULL,
    QUADRA,
    DUQUE,
    SEGUIDA,
    QUINA,
    TERNO,
    QUADRADA,
    SENA,
    GENERAL
}
data class Category(
    val type: CategoryType,
    val name: String,
    var score: Int? = null,
    val isScored: Boolean = false,
    val isScratch: Boolean = false // <-- PROPRIEDADE ADICIONADA
) : Serializable