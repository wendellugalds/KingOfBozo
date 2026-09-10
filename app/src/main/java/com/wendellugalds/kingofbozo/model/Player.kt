package com.wendellugalds.kingofbozo.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") var name: String,
    @SerializedName("age") var age: Int,
    @SerializedName("imageUri") var imageUri: String? = null,

    // --- PROPRIEDADES ADICIONADAS PARA ESTATÍSTICAS ---
    @SerializedName("wins") var wins: Int = 0,
    @SerializedName("totalRounds") var totalRounds: Int = 0,
    @SerializedName("risksTaken") var risksTaken: Int = 0,
    @SerializedName("totalPoints") var totalPoints: Int = 0,
    @SerializedName("generals") var generals: Int = 0,
    @SerializedName("mouthPlays") var mouthPlays: Int = 0
) : Serializable