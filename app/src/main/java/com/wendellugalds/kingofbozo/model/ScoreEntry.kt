package com.wendellugalds.kingofbozo.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class ScoreEntry(
    val value: Int,
    val isScratch: Boolean = false,
    val isBoca: Boolean = false,
    val isScored: Boolean = true
) : Serializable