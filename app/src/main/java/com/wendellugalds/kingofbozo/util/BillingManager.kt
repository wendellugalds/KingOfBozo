package com.wendellugalds.kingofbozo.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

object BillingManager : PurchasesUpdatedListener {

    private const val TAG = "BillingManager"
    const val PRODUCT_ID_PREMIUM = "king_of_bozo_premium"

    private var billingClient: BillingClient? = null
    private var premiumProductDetails: ProductDetails? = null
    private var onPurchaseSuccessCallback: (() -> Unit)? = null
    private var onPurchaseErrorCallback: ((String) -> Unit)? = null

    fun initialize(context: Context) {
        if (billingClient != null) return

        val appContext = context.applicationContext
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        startConnection(appContext)
    }

    private fun startConnection(context: Context) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryActivePurchases(context)
                    queryProductDetails()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Tenta reconectar em caso de desconexão
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    private fun queryActivePurchases(context: Context) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var isPremiumActive = false
                for (purchase in purchases) {
                    if (purchase.products.contains(PRODUCT_ID_PREMIUM) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        isPremiumActive = true
                        handlePurchase(context, purchase, null)
                    }
                }
                if (isPremiumActive) {
                    PremiumManager.setUserPremium(context, true)
                }
            }
        }
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (productDetails in productDetailsList) {
                    if (productDetails.productId == PRODUCT_ID_PREMIUM) {
                        premiumProductDetails = productDetails
                    }
                }
            }
        }
    }

    fun getFormattedPrice(): String? {
        return premiumProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    fun launchBillingFlow(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val productDetails = premiumProductDetails
        if (productDetails == null || billingClient?.isReady != true) {
            // Se o Play Console ainda não estiver vinculado/offline, abre fluxo gracioso/fallback
            onError("Produto não encontrado ou Play Store desconectada")
            return
        }

        onPurchaseSuccessCallback = onSuccess
        onPurchaseErrorCallback = onError

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(PRODUCT_ID_PREMIUM)) {
                    handlePurchase(null, purchase, onPurchaseSuccessCallback)
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onPurchaseErrorCallback?.invoke("Compra cancelada pelo usuário")
        } else {
            onPurchaseErrorCallback?.invoke("Erro na compra: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(context: Context?, purchase: Purchase, onSuccess: (() -> Unit)?) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            context?.let { PremiumManager.setUserPremium(it, true) }

            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        context?.let { PremiumManager.setUserPremium(it, true) }
                        onSuccess?.invoke()
                    }
                }
            } else {
                onSuccess?.invoke()
            }
        }
    }
}
