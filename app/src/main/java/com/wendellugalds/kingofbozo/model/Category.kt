package com.wendellugalds.kingofbozo.model

import java.io.Serializable

data class Category(
    val type: CategoryType,
    val name: String,
    var score: Int? = null,
    val isScored: Boolean = false,
    val isScratch: Boolean = false,
    val isBoca: Boolean = false
) : Serializable