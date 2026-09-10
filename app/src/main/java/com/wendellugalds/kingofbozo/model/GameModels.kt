package com.wendellugalds.kingofbozo.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
enum class CategoryType {
    @SerializedName("AS") AS,
    @SerializedName("DUQUE") DUQUE,
    @SerializedName("TERNO") TERNO,
    @SerializedName("QUADRA") QUADRA,
    @SerializedName("QUINA") QUINA,
    @SerializedName("SENA") SENA,
    @SerializedName("FULL") FULL,
    @SerializedName("SEGUIDA") SEGUIDA,
    @SerializedName("QUADRADA") QUADRADA,
    @SerializedName("GENERAL") GENERAL
}

@Keep
enum class GameStatus {
    @SerializedName("ONGOING") ONGOING,
    @SerializedName("WAITING_TIE_BREAKER") WAITING_TIE_BREAKER,
    @SerializedName("FINISHED") FINISHED
}

@Keep
data class PlayerScore(
    @SerializedName("name") val name: String,
    @SerializedName("totalScore") var totalScore: Int
)