package com.wendellugalds.kingofbozo.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wendellugalds.kingofbozo.data.PlayerRepository // <-- IMPORT CORRIGIDO

class PlayerViewModelFactory(private val repository: PlayerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}