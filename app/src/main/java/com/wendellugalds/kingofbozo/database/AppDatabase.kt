package com.wendellugalds.kingofbozo.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wendellugalds.kingofbozo.model.Player

// A versão do banco de dados permanece em 3.
@Database(entities = [Player::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // A MIGRAÇÃO MANUAL FOI REMOVIDA DAQUI

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "king_of_bozo_database"
                )
                    // --- ESTA É A LINHA QUE VOCÊ LEMBRAVA ---
                    // Ela substitui o .addMigrations() e resolve o problema.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}