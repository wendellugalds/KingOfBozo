package com.wendellugalds.kingofbozo.util

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.button.MaterialButton
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.ui.PremiumBottomSheet

object AdManager {

    private const val ADMOB_REWARDED_UNIT_ID = "ca-app-pub-8591104286915086/5992681322"

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
            ADMOB_REWARDED_UNIT_ID,
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

        showTemporaryAccessDialog(
            activity,
            fragmentManager,
            activity.getString(R.string.ad_rewarded_ranking_msg)
        ) {
            isRankingUnlockedTemp = true
            onAccessGranted()
        }
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

        showTemporaryAccessDialog(
            activity,
            fragmentManager,
            activity.getString(R.string.ad_rewarded_round_msg)
        ) {
            onAccessGranted()
        }
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

        showTemporaryAccessDialog(
            activity,
            fragmentManager,
            activity.getString(R.string.ad_rewarded_round_msg)
        ) {
            isRoundsUnlockedTemp = true
            onAccessGranted()
        }
    }

    private fun showTemporaryAccessDialog(
        activity: Activity,
        fragmentManager: FragmentManager,
        message: String,
        onRewardEarned: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_temporary_access, null)
        val dialog = AlertDialog.Builder(activity, R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val messageView = dialogView.findViewById<TextView>(R.id.dialog_message)
        val btnWatch = dialogView.findViewById<MaterialButton>(R.id.btn_watch)
        val btnPremium = dialogView.findViewById<MaterialButton>(R.id.btn_premium)
        val btnCancel = dialogView.findViewById<ImageView>(R.id.btn_cancel)

        titleView.text = activity.getString(R.string.ad_rewarded_dialog_title)
        messageView.text = message

        btnWatch.setOnClickListener {
            dialog.dismiss()
            showRewardedAd(activity) {
                Toast.makeText(activity, activity.getString(R.string.ad_rewarded_unlocked_toast), Toast.LENGTH_SHORT).show()
                onRewardEarned()
            }
        }

        btnPremium.setOnClickListener {
            dialog.dismiss()
            val premiumSheet = PremiumBottomSheet()
            premiumSheet.show(fragmentManager, "PremiumBottomSheet")
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
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
