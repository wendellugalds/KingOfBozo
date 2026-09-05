package com.wendellugalds.kingofbozo.util

import android.accounts.AccountManager
import android.content.Context

object PremiumManager {

    private const val PREFS_NAME = "premium_prefs"
    private const val KEY_IS_PREMIUM = "is_user_premium"

    const val DEVELOPER_EMAIL = "wendellugaldsg@gmail.com"

    const val MAX_FREE_PLAYERS = 3
    const val MAX_FREE_SAVED_GAMES = 1

    fun isUserPremium(context: Context): Boolean {
        // Bypass automático do desenvolvedor (wendellugaldsg@gmail.com)
        if (checkIsDeveloperAccount(context)) {
            return true
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    fun checkIsDeveloperAccount(context: Context): Boolean {
        try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            for (account in accounts) {
                if (account.name.equals(DEVELOPER_EMAIL, ignoreCase = true)) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
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
