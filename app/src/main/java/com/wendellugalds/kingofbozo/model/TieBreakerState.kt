package com.wendellugalds.kingofbozo.model

/**
 * Representa os possíveis estados do fluxo de desempate.
 */
sealed class TieBreakerState {
    object Idle : TieBreakerState()
    data class ShowTiedPlayers(val tiedPlayers: List<PlayerState>) : TieBreakerState()
    data class ShowScoreInput(val player: PlayerState) : TieBreakerState()
    object ConfirmExit : TieBreakerState()
}
