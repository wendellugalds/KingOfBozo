package com.wendellugalds.kingofbozo.util

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.ui.PremiumBottomSheet

object AdManager {

    // ID de teste oficial do Google AdMob para Vídeo Recompensado
    private const val ADMOB_REWARDED_TEST_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    const val MAX_FREE_ROUNDS_PER_MATCH = 3

    var isRankingUnlockedTemp: Boolean = false
    var isRoundsUnlockedTemp: Boolean = false

    private var rewardedAd: RewardedAd? = null
    private var isLoadingAd = false

    fun initAdMob(context: Context) {
        MobileAds.initialize(context) {
            loadRewardedAd(context)
        }
    }

    fun resetSessionTempAccess() {
        isRankingUnlockedTemp = false
        isRoundsUnlockedTemp = false
    }

    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isLoadingAd) return
        isLoadingAd = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            ADMOB_REWARDED_TEST_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingAd = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingAd = false
                }
            }
        )
    }

    fun checkAndShowRankingAd(
        activity: Activity,
        fragmentManager: FragmentManager,
        onAccessGranted: () -> Unit
    ) {
        // Se for Premium, libera o acesso direto sem anúncio
        if (PremiumManager.isUserPremium(activity)) {
            onAccessGranted()
            return
        }

        MaterialAlertDialogBuilder(activity, R.style.CustomAlertDialog)
            .setTitle(activity.getString(R.string.ad_rewarded_dialog_title))
            .setMessage(activity.getString(R.string.ad_rewarded_ranking_msg))
            .setPositiveButton(activity.getString(R.string.ad_rewarded_btn_watch)) { dialog, _ ->
                dialog.dismiss()
                showRewardedAd(activity) {
                    isRankingUnlockedTemp = true
                    Toast.makeText(activity, activity.getString(R.string.ad_rewarded_unlocked_toast), Toast.LENGTH_SHORT).show()
                    onAccessGranted()
                }
            }
            .setNeutralButton(activity.getString(R.string.ad_rewarded_btn_premium)) { dialog, _ ->
                dialog.dismiss()
                val premiumSheet = PremiumBottomSheet()
                premiumSheet.show(fragmentManager, "PremiumBottomSheet")
            }
            .setNegativeButton(activity.getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun checkAndShowNextRoundAd(
        activity: Activity,
        fragmentManager: FragmentManager,
        onAccessGranted: () -> Unit
    ) {
        if (PremiumManager.isUserPremium(activity)) {
            onAccessGranted()
            return
        }

        MaterialAlertDialogBuilder(activity, R.style.CustomAlertDialog)
            .setTitle(activity.getString(R.string.ad_rewarded_dialog_title))
            .setMessage(activity.getString(R.string.ad_rewarded_round_msg))
            .setPositiveButton(activity.getString(R.string.ad_rewarded_btn_watch)) { dialog, _ ->
                dialog.dismiss()
                showRewardedAd(activity) {
                    Toast.makeText(activity, activity.getString(R.string.ad_rewarded_unlocked_toast), Toast.LENGTH_SHORT).show()
                    onAccessGranted()
                }
            }
            .setNeutralButton(activity.getString(R.string.ad_rewarded_btn_premium)) { dialog, _ ->
                dialog.dismiss()
                val premiumSheet = PremiumBottomSheet()
                premiumSheet.show(fragmentManager, "PremiumBottomSheet")
            }
            .setNegativeButton(activity.getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun checkAndShowRoundLimitAd(
        activity: Activity,
        fragmentManager: FragmentManager,
        currentRound: Int,
        onAccessGranted: () -> Unit
    ) {
        // Se for Premium ou se a rodada é <= limite gratuito ou se já liberou temporariamente nesta partida
        if (PremiumManager.isUserPremium(activity) || currentRound <= MAX_FREE_ROUNDS_PER_MATCH || isRoundsUnlockedTemp) {
            onAccessGranted()
            return
        }

        MaterialAlertDialogBuilder(activity, R.style.CustomAlertDialog)
            .setTitle(activity.getString(R.string.ad_rewarded_dialog_title))
            .setMessage(activity.getString(R.string.ad_rewarded_round_msg))
            .setPositiveButton(activity.getString(R.string.ad_rewarded_btn_watch)) { dialog, _ ->
                dialog.dismiss()
                showRewardedAd(activity) {
                    isRoundsUnlockedTemp = true
                    Toast.makeText(activity, activity.getString(R.string.ad_rewarded_unlocked_toast), Toast.LENGTH_SHORT).show()
                    onAccessGranted()
                }
            }
            .setNeutralButton(activity.getString(R.string.ad_rewarded_btn_premium)) { dialog, _ ->
                dialog.dismiss()
                val premiumSheet = PremiumBottomSheet()
                premiumSheet.show(fragmentManager, "PremiumBottomSheet")
            }
            .setNegativeButton(activity.getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showRewardedAd(activity: Activity, onRewardGranted: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { _ ->
                rewardedAd = null
                loadRewardedAd(activity)
                onRewardGranted()
            }
        } else {
            // Fallback gracioso para testes/offline caso o anúncio ainda não tenha carregado
            loadRewardedAd(activity)
            onRewardGranted()
        }
    }
}
