package com.wendellugalds.kingofbozo

import android.app.Application
import com.wendellugalds.kingofbozo.data.PlayerRepository
import com.wendellugalds.kingofbozo.database.AppDatabase
import com.wendellugalds.kingofbozo.util.ThemeStorage

class PlayersApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { PlayerRepository(database.playerDao(), database.savedGameDao(), this) }

    override fun onCreate() {
        super.onCreate()
        ThemeStorage.applySettings(this)
    }
}
