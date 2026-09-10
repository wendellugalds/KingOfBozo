package com.wendellugalds.kingofbozo.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class ScoreEntry(
    @SerializedName("value") val value: Int,
    @SerializedName("isScratch") val isScratch: Boolean = false,
    @SerializedName("isBoca") val isBoca: Boolean = false,
    @SerializedName("isScored") val isScored: Boolean = true
) : Serializable