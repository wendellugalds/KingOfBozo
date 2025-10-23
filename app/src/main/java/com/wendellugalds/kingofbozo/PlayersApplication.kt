package com.wendellugalds.kingofbozo

import android.app.Application
import com.wendellugalds.kingofbozo.data.PlayerRepository // <-- IMPORT ADICIONADO
import com.wendellugalds.kingofbozo.database.AppDatabase

class PlayersApplication : Application() {
    // A instância do banco de dados
    private val database by lazy { AppDatabase.getDatabase(this) }

    // O repositório que usa o DAO do banco de dados
    val repository by lazy { PlayerRepository(database.playerDao()) }
}