package com.wendellugalds.kingofbozo.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.button.MaterialButton
import com.wendellugalds.kingofbozo.R
import com.wendellugalds.kingofbozo.ui.PremiumBottomSheet

object AdManager {

    // ID Real de Produção para Rewarded Ads
    private const val ADMOB_REWARDED_UNIT_ID = "ca-app-pub-4216110504669009/8344709002"
    private const val TAG = "AdManagerDebug"

    var isRankingUnlockedTemp: Boolean = false
    var isRoundsUnlockedTemp: Boolean = false

    private var rewardedAd: RewardedAd? = null
    private var isLoadingAd = false

    fun initAdMob(context: Context) {
        Log.d(TAG, "Inicializando MobileAds...")
        MobileAds.initialize(context) {
            Log.d(TAG, "MobileAds inicializado com sucesso.")
            loadRewardedAd(context)
        }
    }

    fun resetSessionTempAccess() {
        isRankingUnlockedTemp = false
        isRoundsUnlockedTemp = false
    }

    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null) {
            Log.d(TAG, "loadRewardedAd ignorado: Anúncio já está em memória.")
            return
        }
        if (isLoadingAd) {
            Log.d(TAG, "loadRewardedAd ignorado: Anúncio já está sendo carregado...")
            return
        }
        isLoadingAd = true
        Log.d(TAG, "Iniciando requisição de carregamento do anúncio premiado...")

        val adRequest = AdRequest.Builder().build()

        MobileAds.initialize(context) {
            Log.d(TAG, "MobileAds inicializado com sucesso.")
        }

        RewardedAd.load(
            context.applicationContext,
            ADMOB_REWARDED_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingAd = false
                    Log.d(TAG, "SUCESSO: Anúncio carregado com sucesso!")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingAd = false
                    Log.e(TAG, "FALHA ADMOB -> Código: ${error.code} | Mensagem: ${error.message} | Domínio: ${error.domain} | Causa: ${error.cause}")
                }
            }
        )
    }

    fun checkAndShowRankingAd(
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

        if (rewardedAd == null) {
            loadRewardedAd(activity)
        }

        btnWatch.setOnClickListener {
            val ad = rewardedAd
            if (ad != null) {
                dialog.dismiss()
                showAdWithCallbacks(activity, ad, onRewardEarned)
            } else {
                Toast.makeText(activity, "A carregar anúncio do servidor... Aguarde um instante.", Toast.LENGTH_SHORT).show()

                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    activity,
                    ADMOB_REWARDED_UNIT_ID,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(loadedAd: RewardedAd) {
                            rewardedAd = loadedAd
                            if (activity.isFinishing || activity.isDestroyed) return
                            dialog.dismiss()
                            showAdWithCallbacks(activity, loadedAd, onRewardEarned)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            rewardedAd = null
                            Log.e(TAG, "FALHA ADMOB NO CLIQUE -> Código: ${error.code} | Mensagem: ${error.message} | Domínio: ${error.domain} | Causa: ${error.cause}")
                            Toast.makeText(activity, "Falha ao carregar anúncio. Verifique a sua conexão.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
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

    private fun showAdWithCallbacks(
        activity: Activity,
        ad: RewardedAd,
        onRewardEarned: () -> Unit
    ) {
        var rewardEarned = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Anúncio fechado pelo utilizador.")
                rewardedAd = null
                loadRewardedAd(activity) // Carrega o próximo em segundo plano imediatamente!

                if (rewardEarned) {
                    Toast.makeText(activity, activity.getString(R.string.ad_rewarded_unlocked_toast), Toast.LENGTH_SHORT).show()
                    onRewardEarned()
                } else {
                    Log.d(TAG, "Anúncio fechado sem assistir até o fim. Recompensa negada.")
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Falha ao exibir anúncio: ${adError.message}")
                rewardedAd = null
                loadRewardedAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Anúncio exibido na tela.")
            }
        }

        ad.show(activity) { _ ->
            Log.d(TAG, "Recompensa concedida pelo Google!")
            rewardEarned = true
        }
    }
}
