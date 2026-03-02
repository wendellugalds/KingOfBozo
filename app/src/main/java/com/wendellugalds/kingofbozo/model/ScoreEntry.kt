package com.wendellugalds.kingofbozo.model

import java.io.Serializable

data class ScoreEntry(
    val value: Int,
    val isScratch: Boolean = false,
    val isBoca: Boolean = false,
    val isScored: Boolean = true
) : Serializable