package com.wendellugalds.kingofbozo.util

import android.content.Context
import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.R

object PremiumManager {

    private const val PREFS_NAME = "premium_prefs"
    private const val KEY_IS_PREMIUM = "is_user_premium"

    const val MAX_FREE_PLAYERS = 3
    const val MAX_FREE_SAVED_GAMES = 1

    fun isUserPremium(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    fun setUserPremium(context: Context, isPremium: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
    }

    fun updatePremiumButton(button: ImageView) {
        val context = button.context
        val isPremium = isUserPremium(context)
        if (isPremium) {
            button.backgroundTintList = null
            button.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.premium_text_on_gold)
            )
        } else {
            val cardBgColor = MaterialColors.getColor(context, R.attr.cardBackgroundColor, 0)
            button.backgroundTintList = ColorStateList.valueOf(cardBgColor)
            button.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.premium)
            )
        }
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
