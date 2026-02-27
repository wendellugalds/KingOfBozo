package com.wendellugalds.kingofbozo.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wendellugalds.kingofbozo.data.PlayerRepository
import com.wendellugalds.kingofbozo.model.Category
import com.wendellugalds.kingofbozo.model.CategoryType
import com.wendellugalds.kingofbozo.model.GameState
import com.wendellugalds.kingofbozo.model.Player
import com.wendellugalds.kingofbozo.model.PlayerState
import com.wendellugalds.kingofbozo.model.SavedGame
import com.wendellugalds.kingofbozo.model.ScoreEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(private val repository: PlayerRepository) : ViewModel() {

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _openedCategory = MutableLiveData<Category?>()
    val openedCategory: LiveData<Category?> = _openedCategory

    val availablePlayers: LiveData<List<Player>> = repository.allPlayers.asLiveData()
    private val _selectedPlayers = MutableLiveData<List<Player>>(emptyList())
    val selectedPlayers: LiveData<List<Player>> = _selectedPlayers

    val sortedPlayerListForSelection = MediatorLiveData<List<Player>>()

    private val _navigateToRanking = MutableLiveData<Boolean>()
    val navigateToRanking: LiveData<Boolean> = _navigateToRanking

    private val _gameSavedSuccessfully = MutableLiveData<Boolean>()
    val gameSavedSuccessfully: LiveData<Boolean> = _gameSavedSuccessfully
    
    val allSavedGames: LiveData<List<SavedGame>> = repository.allSavedGames.asLiveData()

    private var isFinishingRound = false

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
            if (currentSelection.size < 9) {
                currentSelection.add(player)
            }
        }
        _selectedPlayers.value = currentSelection
    }

    fun clearSelection() {
        _selectedPlayers.value = emptyList()
    }

    fun startGame() {
        isFinishingRound = false
        val playersToStart = _selectedPlayers.value.orEmpty()
        if (playersToStart.isEmpty()) return
        val initialPlayersState = playersToStart.map { player ->
            val initialScores = CategoryType.values().associateWith { null as ScoreEntry? }.toMutableMap()
            PlayerState(
                playerName = player.name,
                playerImage = player.imageUri,
                scores = initialScores,
                totalWins = player.wins,
                totalPoints = player.totalPoints
            )
        }
        _gameState.value = GameState(gameId = 0, playersState = initialPlayersState, currentPlayerIndex = 0, currentRound = 1)
        _navigateToRanking.value = false
    }

    // --- GERENCIAMENTO DE JOGADORES NO JOGO ATUAL ---

    fun addPlayerToCurrentGame(player: Player) {
        val currentState = _gameState.value ?: return
        val alreadyInGame = currentState.playersState.any { it.playerName == player.name }
        if (alreadyInGame) return

        val initialScores = CategoryType.values().associateWith { null as ScoreEntry? }.toMutableMap()
        val newPlayerState = PlayerState(
            playerName = player.name,
            playerImage = player.imageUri,
            scores = initialScores,
            totalWins = player.wins,
            totalPoints = player.totalPoints
        )

        val updatedPlayers = currentState.playersState.toMutableList()
        updatedPlayers.add(newPlayerState)

        _gameState.value = currentState.copy(playersState = updatedPlayers)
        saveCurrentGame()
    }

    fun removePlayerFromCurrentGame(playerName: String) {
        val currentState = _gameState.value ?: return
        if (currentState.playersState.size <= 1) return // Não permite remover o último jogador

        val updatedPlayers = currentState.playersState.filter { it.playerName != playerName }
        
        var newCurrentIndex = currentState.currentPlayerIndex
        if (newCurrentIndex >= updatedPlayers.size) {
            newCurrentIndex = 0
        }

        _gameState.value = currentState.copy(
            playersState = updatedPlayers,
            currentPlayerIndex = newCurrentIndex
        )
        saveCurrentGame()
    }

    fun setCurrentPlayer(playerIndex: Int) {
        val currentState = _gameState.value ?: return
        if (playerIndex < 0 || playerIndex >= currentState.playersState.size) return
        _gameState.value = currentState.copy(currentPlayerIndex = playerIndex)
    }

    fun submitScore(categoryType: CategoryType, score: Int, isScratch: Boolean = false, shouldAutoAdvance: Boolean = true, isClear: Boolean = false) {
        viewModelScope.launch {
            if (isFinishingRound) return@launch
            val currentState = _gameState.value ?: return@launch
            val currentPlayerIndex = currentState.currentPlayerIndex

            val updatedPlayers = currentState.playersState.mapIndexed { index, playerState ->
                if (index == currentPlayerIndex) {
                    val newScores = playerState.scores.toMutableMap()
                    val oldScore = newScores[categoryType]?.value ?: 0
                    
                    if (isClear) {
                        newScores[categoryType] = null
                    } else {
                        newScores[categoryType] = ScoreEntry(value = score, isScratch = isScratch)
                    }
                    
                    playerState.copy(
                        scores = newScores,
                        totalScore = playerState.totalScore - oldScore + (if (isClear) 0 else score)
                    )
                } else {
                    playerState
                }
            }

            val newState = currentState.copy(playersState = updatedPlayers)
            _gameState.value = newState

            // Se for LIMPAR, não faz mais nada (não finaliza, não avança)
            if (isClear) return@launch

            // Regra: General de Boca (1000 pontos) finaliza instantaneamente a rodada.
            if (categoryType == CategoryType.GENERAL && score == 1000) {
                finishRound(newState)
            } else if (isRoundOver(newState)) {
                // Se todos os jogadores preencheram todos os botões, a rodada finaliza.
                finishRound(newState)
            } else if (shouldAutoAdvance) {
                // Se não finalizou a rodada, avança para o próximo jogador.
                delay(500)
                val stateAfterDelay = _gameState.value ?: return@launch
                val nextPlayerIndex = (currentPlayerIndex + 1) % stateAfterDelay.playersState.size
                _gameState.value = stateAfterDelay.copy(currentPlayerIndex = nextPlayerIndex)
            }
        }
    }

    fun forceFinishRound() {
        _gameState.value?.let { finishRound(it) }
    }

    private fun isRoundOver(state: GameState): Boolean {
        // Uma rodada só acaba se TODOS os jogadores preencheram TODAS as 10 categorias.
        return state.playersState.all { player ->
            player.scores.values.count { it != null } == 10
        }
    }

    private fun finishRound(state: GameState) {
        if (isFinishingRound) return
        isFinishingRound = true
        
        viewModelScope.launch {
            val winner = state.playersState.maxByOrNull { it.totalScore }
            val allPlayersInDb = repository.allPlayers.first()
            
            val updatedPlayersState = state.playersState.map { playerState ->
                val isWinner = playerState.playerName == winner?.playerName
                val newSessionWins = if (isWinner) playerState.sessionWins + 1 else playerState.sessionWins
                val newSessionPoints = playerState.sessionTotalPoints + playerState.totalScore
                
                // Buscar jogador no DB de forma mais robusta (por nome ou id se disponível no PlayerState)
                val dbPlayer = allPlayersInDb.find { it.name == playerState.playerName }
                var updatedTotalWins = playerState.totalWins
                var updatedTotalPoints = playerState.totalPoints
                
                dbPlayer?.let { player ->
                    player.totalPoints += playerState.totalScore
                    player.totalRounds += 1
                    
                    if (isWinner) {
                        player.wins += 1
                    }
                    
                    updatedTotalWins = player.wins
                    updatedTotalPoints = player.totalPoints

                    // Contar generais (valor > 0 e não riscado)
                    val roundGenerals = if ((playerState.scores[CategoryType.GENERAL]?.value ?: 0) > 0 && playerState.scores[CategoryType.GENERAL]?.isScratch == false) 1 else 0
                    player.generals += roundGenerals

                    // Contar jogadas de "boca"
                    var roundMouthPlays = 0
                    playerState.scores.forEach { (type, entry) ->
                        val value = entry?.value ?: 0
                        if (value > 0 && entry?.isScratch == false) {
                            val isBoca = when(type) {
                                CategoryType.FULL -> value == 15
                                CategoryType.SEGUIDA -> value == 25
                                CategoryType.QUADRADA -> value == 35
                                CategoryType.GENERAL -> value == 1000
                                else -> false
                            }
                            if (isBoca) roundMouthPlays++
                        }
                    }
                    player.mouthPlays += roundMouthPlays

                    // Contar riscos
                    val roundRisks = playerState.scores.values.count { entry -> entry?.isScratch == true }
                    player.risksTaken += roundRisks

                    repository.update(player)
                }

                playerState.copy(
                    sessionWins = newSessionWins,
                    sessionTotalPoints = newSessionPoints,
                    totalWins = updatedTotalWins,
                    totalPoints = updatedTotalPoints
                )
            }
            
            _gameState.value = state.copy(playersState = updatedPlayersState)
            _navigateToRanking.value = true
            
            // Auto-salva após processar o fim da rodada para persistir os totais de sessão
            saveCurrentGame()
        }
    }

    fun startNextRound() {
        isFinishingRound = false
        val currentState = _gameState.value ?: return
        _gameState.value = prepareNextRound(currentState)
        _navigateToRanking.value = false
        saveCurrentGame()
    }

    private fun prepareNextRound(state: GameState): GameState {
        val nextRound = state.currentRound + 1
        val sortedPlayers = state.playersState.sortedByDescending { it.totalScore }
        
        val newPlayersState = sortedPlayers.map { player ->
            PlayerState(
                playerName = player.playerName,
                playerImage = player.playerImage,
                scores = CategoryType.values().associateWith { null as ScoreEntry? }.toMutableMap(),
                totalScore = 0,
                sessionWins = player.sessionWins,
                sessionTotalPoints = player.sessionTotalPoints,
                totalWins = player.totalWins,
                totalPoints = player.totalPoints
            )
        }
        return GameState(
            gameId = state.gameId,
            playersState = newPlayersState, 
            currentPlayerIndex = 0, 
            currentRound = nextRound,
            startTimeMillis = System.currentTimeMillis(),
            accumulatedTimeMillis = state.accumulatedTimeMillis + (System.currentTimeMillis() - state.startTimeMillis)
        )
    }

    fun saveCurrentGame() {
        val state = _gameState.value ?: return
        viewModelScope.launch {
            val gson = Gson()
            val playerStatesJson = gson.toJson(state.playersState)
            val savedGame = SavedGame(
                id = state.gameId,
                currentRound = state.currentRound,
                playerStatesJson = playerStatesJson,
                accumulatedTimeMillis = getCurrentAccumulatedTime(),
                currentPlayerIndex = state.currentPlayerIndex
            )
            val newId = repository.saveGame(savedGame)
            if (state.gameId == 0L) {
                _gameState.value = state.copy(gameId = newId)
            }
            _gameSavedSuccessfully.value = true
        }
    }
    
    fun deleteSavedGame(savedGame: SavedGame) {
        viewModelScope.launch {
            repository.deleteSavedGame(savedGame)
        }
    }

    fun loadGame(savedGame: SavedGame) {
        isFinishingRound = false
        val gson = Gson()
        val listType = object : TypeToken<List<PlayerState>>() {}.type
        val playerStates: List<PlayerState> = gson.fromJson(savedGame.playerStatesJson, listType)
        
        val state = GameState(
            gameId = savedGame.id,
            playersState = playerStates,
            currentPlayerIndex = savedGame.currentPlayerIndex,
            currentRound = savedGame.currentRound,
            startTimeMillis = System.currentTimeMillis(),
            accumulatedTimeMillis = savedGame.accumulatedTimeMillis
        )
        _gameState.value = state

        val hasGeneralBoca = playerStates.any { it.scores[CategoryType.GENERAL]?.value == 1000 }
        
        if (hasGeneralBoca) {
            startNextRound()
        } else {
            _navigateToRanking.value = false
        }
    }

    fun onRankingNavigated() {
        _navigateToRanking.value = false
    }

    fun resetSaveStatus() {
        _gameSavedSuccessfully.value = false
    }

    fun getCurrentAccumulatedTime(): Long {
        val state = _gameState.value ?: return 0
        return state.accumulatedTimeMillis + (System.currentTimeMillis() - state.startTimeMillis)
    }
}
