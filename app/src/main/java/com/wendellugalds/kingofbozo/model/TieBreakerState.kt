package com.wendellugalds.kingofbozo.model

import androidx.annotation.Keep

/**
 * Representa os possíveis estados do fluxo de desempate.
 */
@Keep
sealed class TieBreakerState {
    @Keep object Idle : TieBreakerState()
    @Keep data class ShowTiedPlayers(val tiedPlayers: List<PlayerState>) : TieBreakerState()
    @Keep data class ShowScoreInput(val player: PlayerState) : TieBreakerState()
    @Keep object ConfirmExit : TieBreakerState()
}
