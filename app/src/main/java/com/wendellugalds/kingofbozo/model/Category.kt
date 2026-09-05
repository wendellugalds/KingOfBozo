package com.wendellugalds.kingofbozo.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class Category(
    val type: CategoryType,
    val name: String,
    var score: Int? = null,
    val isScored: Boolean = false,
    val isScratch: Boolean = false,
    val isBoca: Boolean = false
) : Serializable