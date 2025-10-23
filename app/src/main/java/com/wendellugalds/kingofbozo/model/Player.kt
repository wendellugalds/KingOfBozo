package com.wendellugalds.kingofbozo.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String,
    var age: Int,
    var imageUri: String? = null,

    // --- PROPRIEDADES ADICIONADAS PARA ESTATÍSTICAS ---
    var wins: Int = 0,
    var totalRounds: Int = 0,
    var risksTaken: Int = 0,
    var totalPoints: Int = 0,
    var generals: Int = 0,
    var mouthPlays: Int = 0
) : Serializable