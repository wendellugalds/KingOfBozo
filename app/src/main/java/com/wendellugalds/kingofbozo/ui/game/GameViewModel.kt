package com.wendellugalds.kingofbozo.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.wendellugalds.kingofbozo.data.PlayerRepository
import com.wendellugalds.kingofbozo.model.Category
import com.wendellugalds.kingofbozo.model.CategoryType
import com.wendellugalds.kingofbozo.model.GameState
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.model.ScoreEntry

class GameViewModel(private val repository: PlayerRepository) : ViewModel() {

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _openedCategory = MutableLiveData<Category?>()
    val openedCategory: LiveData<Category?> = _openedCategory

    val availablePlayers: LiveData<List<Player>> = repository.allPlayers.asLiveData()
    private val _selectedPlayers = MutableLiveData<List<Player>>(emptyList())
    val selectedPlayers: LiveData<List<Player>> = _selectedPlayers

    val sortedPlayerListForSelection = MediatorLiveData<List<Player>>()

    init {
        val merger = {
            val allPlayers = availablePlayers.value.orEmpty()
            val currentlySelected = selectedPlayers.value.orEmpty()
            val unselectedPlayers = allPlayers.filter { it !in currentlySelected }
            sortedPlayerListForSelection.value = currentlySelected + unselectedPlayers
        }
        sortedPlayerListForSelection.addSource(availablePlayers) { merger() }
        sortedPlayerListForSelection.addSource(selectedPlayers) { merger() }
    }

    fun loadPlayersForGame(ids: List<Long>): LiveData<List<Player>> {
        return repository.getPlayersByIds(ids).asLiveData()
    }

    fun togglePlayerSelection(player: Player) {
        val currentSelection = _selectedPlayers.value?.toMutableList() ?: mutableListOf()
        if (currentSelection.contains(player)) {
            currentSelection.remove(player)
        } else {
            currentSelection.add(player)
        }
        _selectedPlayers.value = currentSelection
    }

    fun clearSelection() {
        _selectedPlayers.value = emptyList()
    }

    fun startGame() {
        val playersToStart = _selectedPlayers.value.orEmpty()
        if (playersToStart.isEmpty()) return
        val initialPlayersState = playersToStart.map { player ->
            val initialScores = CategoryType.values().associateWith { null as ScoreEntry? }.toMutableMap()
            PlayerState(playerName = player.name, scores = initialScores)
        }
        _gameState.value = GameState(playersState = initialPlayersState, currentPlayerIndex = 0, currentRound = 1)
    }

    fun setCurrentPlayer(playerIndex: Int) {
        val currentState = _gameState.value ?: return
        if (playerIndex < 0 || playerIndex >= currentState.playersState.size) return
        _gameState.value = currentState.copy(currentPlayerIndex = playerIndex)
    }

    fun getCategoriesForCurrentPlayer(): List<Category> {
        val currentState = gameState.value ?: return emptyList()
        val currentPlayerState = currentState.playersState[currentState.currentPlayerIndex]

        return CategoryType.values().map { type ->
            val scoreEntry = currentPlayerState.scores[type]
            val displayName = when (type) {
                CategoryType.AS -> "Ás"; CategoryType.DUQUE -> "Duque"; CategoryType.TERNO -> "Terno"
                CategoryType.QUADRA -> "Quadra"; CategoryType.QUINA -> "Quina"; CategoryType.SENA -> "Sena"
                CategoryType.FULL -> "Full"; CategoryType.SEGUIDA -> "Seguida"
                CategoryType.QUADRADA -> "Quadrada"; CategoryType.GENERAL -> "General"
            }
            Category(
                type = type,
                name = displayName,
                score = scoreEntry?.value,
                isScored = scoreEntry != null,
                isScratch = scoreEntry?.isScratch ?: false
            )
        }
    }

    fun openCategoryDetail(category: Category) {
        _openedCategory.value = category
    }

    fun closeCategoryDetail() {
        _openedCategory.value = null
    }

    // --- FUNÇÃO ADICIONADA QUE ESTAVA FALTANDO ---
    fun resetScore(categoryType: CategoryType) {
        val currentState = _gameState.value ?: return
        val currentPlayerState = currentState.playersState[currentState.currentPlayerIndex]

        // Remove a pontuação antiga do total
        val oldScore = currentPlayerState.scores[categoryType]?.value ?: 0
        currentPlayerState.totalScore -= oldScore

        // Remove a entrada de pontuação do mapa
        currentPlayerState.scores[categoryType] = null

        // Notifica a UI da mudança
        _gameState.value = currentState

        // Reabre a tela de pontuação para o usuário escolher um novo valor
        val category = getCategoriesForCurrentPlayer().find { it.type == categoryType }
        category?.let { openCategoryDetail(it) }
    }

    fun submitScore(categoryType: CategoryType, score: Int, isScratch: Boolean = false) {
        val currentState = _gameState.value ?: return
        val currentPlayerState = currentState.playersState[currentState.currentPlayerIndex]

        // Pega a pontuação antiga, se houver, para fazer a substituição correta
        val oldScore = currentPlayerState.scores[categoryType]?.value ?: 0

        // Atualiza o placar total: subtrai o valor antigo (que é 0 se for a primeira vez) e soma o novo
        currentPlayerState.totalScore = currentPlayerState.totalScore - oldScore + score

        // Salva a nova pontuação no mapa
        currentPlayerState.scores[categoryType] = ScoreEntry(value = score, isScratch = isScratch)

        // Notifica a UI que o estado do jogo mudou
        _gameState.value = currentState

        closeCategoryDetail()
    }
}