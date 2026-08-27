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
import com.wendellugalds.kingofbozo.model.GameStatus
import com.wendellugalds.kingofbozo.model.TieBreakerState
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
    
    private val _tieBreakerUiState = MutableLiveData<TieBreakerState>(TieBreakerState.Idle)
    val tieBreakerUiState: LiveData<TieBreakerState> = _tieBreakerUiState

    // Armazena os valores extras do dado para desempate temporariamente
    private val _tempTieBreakerValues = MutableLiveData<Map<Long, Int>>(emptyMap())
    val tempTieBreakerValues: LiveData<Map<Long, Int>> = _tempTieBreakerValues

    val allSavedGames: LiveData<List<SavedGame>> = repository.allSavedGames.asLiveData()

    private var isFinishingRound = false

    init {
        // Coletor para manter o GameState sincronizado com o banco de dados em tempo real
        viewModelScope.launch {
            repository.allPlayers.collect { allDbPlayers ->
                _gameState.value?.let { currentState ->
                    val updatedPlayersState = currentState.playersState.map { pState ->
                        val dbPlayer = allDbPlayers.find { it.id == pState.playerId }
                        if (dbPlayer != null && (dbPlayer.name != pState.playerName || dbPlayer.imageUri != pState.playerImage)) {
                            pState.copy(playerName = dbPlayer.name, playerImage = dbPlayer.imageUri)
                        } else {
                            pState
                        }
                    }

                    if (updatedPlayersState != currentState.playersState) {
                        val newState = currentState.copy(playersState = updatedPlayersState)
                        _gameState.value = newState
                        
                        // Sincroniza também o estado de desempate se estiver ativo
                        if (newState.status == GameStatus.WAITING_TIE_BREAKER) {
                            val tiedPlayers = updatedPlayersState.filter { it.playerId in (newState.tiedPlayerIds ?: emptyList()) }
                            _tieBreakerUiState.value = TieBreakerState.ShowTiedPlayers(tiedPlayers)
                        }
                    }
                }
            }
        }

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
        _tieBreakerUiState.value = TieBreakerState.Idle
        _tempTieBreakerValues.value = emptyMap()
        
        val playersToStart = _selectedPlayers.value.orEmpty()
        if (playersToStart.isEmpty()) return
        val initialPlayersState = playersToStart.map { player ->
            val initialScores = CategoryType.values().associateWith { null as ScoreEntry? }.toMutableMap()
            PlayerState(
                playerId = player.id,
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
            playerId = player.id,
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

            // Determina se a pontuação atual é de boca
            val isBoca = when (categoryType) {
                CategoryType.FULL -> score == 15
                CategoryType.SEGUIDA -> score == 25
                CategoryType.QUADRADA -> score == 35
                CategoryType.GENERAL -> score == 1000
                else -> false
            }

            val updatedPlayers = currentState.playersState.mapIndexed { index, playerState ->
                if (index == currentPlayerIndex) {
                    val newScores = playerState.scores.toMutableMap()
                    val oldScore = newScores[categoryType]?.value ?: 0
                    
                    if (isClear) {
                        newScores[categoryType] = null
                    } else {
                        // Persiste o estado isBoca no ScoreEntry
                        newScores[categoryType] = ScoreEntry(
                            value = score, 
                            isScratch = isScratch, 
                            isBoca = isBoca,
                            isScored = true
                        )
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
        
        // 1. Calcula pontuação máxima da rodada atual
        val maxScore = state.playersState.maxOf { it.totalScore }
        
        // 2. Filtra quem atingiu essa pontuação (Potenciais vencedores)
        val winners = state.playersState.filter { it.totalScore == maxScore }

        if (winners.size > 1 && maxScore > 0) {
            // EMPATE DETECTADO NO 1º LUGAR
            val updatedState = state.copy(
                status = GameStatus.WAITING_TIE_BREAKER,
                tiedPlayerIds = winners.map { it.playerId }
            )
            _gameState.value = updatedState
            _tieBreakerUiState.value = TieBreakerState.ShowTiedPlayers(winners)
            saveCurrentGame() // Salva o estado AGUARDANDO_DESEMPATE
            return
        }

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

                    // Contar generais (valor \u003e 0 e não riscado)
                    val roundGenerals = if ((playerState.scores[CategoryType.GENERAL]?.value ?: 0) > 0 && playerState.scores[CategoryType.GENERAL]?.isScratch == false) 1 else 0
                    player.generals += roundGenerals

                    // Contar jogadas de "boca"
                    var roundMouthPlays = 0
                    playerState.scores.forEach { (type, entry) ->
                        if (entry?.isBoca == true) roundMouthPlays++
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
                playerId = player.playerId,
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
                currentPlayerIndex = state.currentPlayerIndex,
                status = state.status,
                tiedPlayerIdsJson = gson.toJson(state.tiedPlayerIds)
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
        _tieBreakerUiState.value = TieBreakerState.Idle
        _tempTieBreakerValues.value = emptyMap()
        
        viewModelScope.launch {
            val gson = Gson()
            val listType = object : TypeToken<List<PlayerState>>() {}.type
            val playerStates: List<PlayerState> = gson.fromJson(savedGame.playerStatesJson, listType)
            
            // Busca a versão mais recente dos jogadores do banco de dados de forma garantida
            val allPlayers = repository.allPlayers.first()
            val syncedPlayerStates = playerStates.map { pState ->
                val dbPlayer = allPlayers.find { it.id == pState.playerId }
                if (dbPlayer != null) {
                    pState.copy(playerName = dbPlayer.name, playerImage = dbPlayer.imageUri)
                } else {
                    pState
                }
            }
            
            val tiedPlayerIds: List<Long>? = gson.fromJson(savedGame.tiedPlayerIdsJson, object : TypeToken<List<Long>>() {}.type)
            
            val state = GameState(
                gameId = savedGame.id,
                playersState = syncedPlayerStates,
                currentPlayerIndex = savedGame.currentPlayerIndex,
                currentRound = savedGame.currentRound,
                startTimeMillis = System.currentTimeMillis(),
                accumulatedTimeMillis = savedGame.accumulatedTimeMillis,
                status = savedGame.status,
                tiedPlayerIds = tiedPlayerIds
            )
            _gameState.value = state

            if (savedGame.status == GameStatus.WAITING_TIE_BREAKER) {
                val tiedPlayers = syncedPlayerStates.filter { it.playerId in (tiedPlayerIds ?: emptyList()) }
                _tieBreakerUiState.value = TieBreakerState.ShowTiedPlayers(tiedPlayers)
                _navigateToRanking.value = false
            } else {
                val hasGeneralBoca = playerStates.any { it.scores[CategoryType.GENERAL]?.value == 1000 }
                if (hasGeneralBoca) {
                    startNextRound()
                } else {
                    _navigateToRanking.value = false
                }
            }
        }
    }

    fun onRankingNavigated() {
        _navigateToRanking.value = false
    }

    // --- LOGICA DE DESEMPATE ---

    fun onPlayerSelectedForTieBreak(player: PlayerState) {
        _tieBreakerUiState.value = TieBreakerState.ShowScoreInput(player)
    }

    fun setTieBreakerValue(player: PlayerState, dieValue: Int) {
        val currentValues = _tempTieBreakerValues.value.orEmpty().toMutableMap()
        currentValues[player.playerId] = dieValue
        _tempTieBreakerValues.value = currentValues
        
        // Volta para a lista de empatados
        val currentState = _gameState.value ?: return
        val tiedPlayers = currentState.playersState.filter { it.playerId in (currentState.tiedPlayerIds ?: emptyList()) }
        _tieBreakerUiState.value = TieBreakerState.ShowTiedPlayers(tiedPlayers)
    }

    fun clearTieBreakerValue(player: PlayerState) {
        val currentValues = _tempTieBreakerValues.value.orEmpty().toMutableMap()
        currentValues.remove(player.playerId)
        _tempTieBreakerValues.value = currentValues
        
        // Volta para a lista de empatados
        val currentState = _gameState.value ?: return
        val tiedPlayers = currentState.playersState.filter { it.playerId in (currentState.tiedPlayerIds ?: emptyList()) }
        _tieBreakerUiState.value = TieBreakerState.ShowTiedPlayers(tiedPlayers)
    }

    fun resolveTie() {
        val currentState = _gameState.value ?: return
        val extraValues = _tempTieBreakerValues.value.orEmpty()
        
        val updatedPlayers = currentState.playersState.map { p ->
            val extra = extraValues[p.playerId] ?: 0
            if (extra > 0) {
                p.copy(totalScore = p.totalScore + extra)
            } else p
        }
        
        val newState = currentState.copy(
            playersState = updatedPlayers,
            status = GameStatus.ONGOING,
            tiedPlayerIds = null
        )
        
        _gameState.value = newState
        _tieBreakerUiState.value = TieBreakerState.Idle
        _tempTieBreakerValues.value = emptyMap()
        
        finishRound(newState)
    }

    fun dismissTieBreaker() {
        if (_gameState.value?.status == GameStatus.WAITING_TIE_BREAKER) {
            _tieBreakerUiState.value = TieBreakerState.ConfirmExit
        } else {
            _tieBreakerUiState.value = TieBreakerState.Idle
        }
    }

    fun cancelExitConfirm() {
        val currentState = _gameState.value ?: return
        if (currentState.status == GameStatus.WAITING_TIE_BREAKER) {
            val tiedPlayers = currentState.playersState.filter { it.playerId in (currentState.tiedPlayerIds ?: emptyList()) }
            _tieBreakerUiState.value = TieBreakerState.ShowTiedPlayers(tiedPlayers)
        } else {
            _tieBreakerUiState.value = TieBreakerState.Idle
        }
    }

    fun resetSaveStatus() {
        _gameSavedSuccessfully.value = false
    }

    fun getCurrentAccumulatedTime(): Long {
        val state = _gameState.value ?: return 0
        return state.accumulatedTimeMillis + (System.currentTimeMillis() - state.startTimeMillis)
    }
}
