package com.wendellugalds.kingofbozo.util

import android.content.Context
import com.wendellugalds.kingofbozo.BuildConfig

object PremiumManager {

    private const val PREFS_NAME = "premium_prefs"
    private const val KEY_IS_PREMIUM = "is_user_premium"

    const val MAX_FREE_PLAYERS = 3
    const val MAX_FREE_SAVED_GAMES = 1

    // =========================================================================
    // FLAG DE TESTE MANUAL (Apenas para modo DEBUG):
    // - Mude para 'true'  -> Força o app a rodar como PREMIUM nos testes
    // - Mude para 'false' -> Força o app a rodar como GRATUITO nos testes
    // - Mude para 'null'  -> Usa o status real gravado no aparelho (SharedPreferences / Compras)
    // =========================================================================
    private val DEBUG_OVERRIDE_PREMIUM: Boolean? = true as Boolean?

    fun isUserPremium(context: Context): Boolean {
        // A flag de teste manual 'DEBUG_OVERRIDE_PREMIUM' SÓ é usada em builds de DEBUG.
        // Em versão de PRODUÇÃO (Release), o código abaixo é ignorado totalmente.
        if (BuildConfig.DEBUG) {
            DEBUG_OVERRIDE_PREMIUM?.let { override ->
                return override
            }
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    fun setUserPremium(context: Context, isPremium: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
    }

    fun podeAdicionarJogador(context: Context, totalAtual: Int): Boolean {
        if (isUserPremium(context)) return true
        return totalAtual < MAX_FREE_PLAYERS
    }

    fun podeSalvarNovoJogo(context: Context, jogosSalvosAtuais: Int): Boolean {
        if (isUserPremium(context)) return true
        return jogosSalvosAtuais < MAX_FREE_SAVED_GAMES
    }
}
