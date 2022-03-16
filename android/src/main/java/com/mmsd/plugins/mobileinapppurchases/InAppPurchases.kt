package com.mmsd.plugins.mobileinapppurchases

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode

class InAppPurchases() : PurchasesUpdatedListener {

    private lateinit var activity: Activity
    private lateinit var billingClient: BillingClient

    private lateinit var purchesProductListener: (BillingResult, MutableList<Purchase>?) -> Unit

    fun echo(value: String): String {
        Log.i("Echo", value)
        return value
    }

    fun connect(context: Context, activity: Activity, callback: (BillingResult) -> Unit) {
        this.activity = activity
        billingClient = BillingClient
        .newBuilder(context)
            .enablePendingPurchases()
            .setListener(this)
            .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                callback(billingResult)
            }

            override fun onBillingServiceDisconnected() {
                Log.i(TAG, "Billing service disconnected")
                val billingResultBuilder = BillingResult.newBuilder()
                billingResultBuilder.setResponseCode(BillingResponseCode.SERVICE_DISCONNECTED)
                billingResultBuilder.setDebugMessage("Service disconnected")
                val billingResult = billingResultBuilder.build()
                callback(billingResult)
            }
        })
    }

    fun purchesProduct(productId: String, skuTypeString: String, listener: (BillingResult, MutableList<Purchase>?) -> Unit) {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready")
            val billingResultBuilder = BillingResult.newBuilder()
            billingResultBuilder.setResponseCode(BillingResponseCode.SERVICE_DISCONNECTED)
            billingResultBuilder.setDebugMessage("Service disconnected")
            val billingResult = billingResultBuilder.build()
            listener(billingResult, null)
        }
        purchesProductListener = listener
        queryProducts(productId, skuTypeString) { result: BillingResult, skuDetails: MutableList<SkuDetails>? ->
            if (skuDetails != null && result.responseCode == BillingResponseCode.OK) {
                launchPurchaseFlow(skuDetails.first())
            } else {
                listener(result, null)
            }
        }
    }

    fun consumeProduct(purchase: Purchase, listener: ConsumeResponseListener) {
        val consumeParams =
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build()
        billingClient.consumeAsync(consumeParams, listener)
    }

    fun acknowledgePurchase(purchase: Purchase, listener: AcknowledgePurchaseResponseListener) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
        billingClient.acknowledgePurchase(acknowledgePurchaseParams.build(), listener)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        purchesProductListener(result, purchases)
    }

    // skuType: BillingClient.SkuType
    private fun queryProducts(productId: String, skuType: String, listener: (BillingResult, MutableList<SkuDetails>?) -> Unit) {
        val skuListToQuery = ArrayList<String>()

        // sku refers to the product ID that was set in the Play Console
        skuListToQuery.add(productId)

        val params = SkuDetailsParams.newBuilder()
        params
            .setSkusList(skuListToQuery)
            .setType(skuType)
        // SkuType.INAPP refers to 'managed products' or one time purchases
        // To query for subscription products, you would use SkuType.SUBS

        billingClient.querySkuDetailsAsync(
            params.build(),
            object : SkuDetailsResponseListener {
                override fun onSkuDetailsResponse(
                    result: BillingResult,
                    skuDetails: MutableList<SkuDetails>?
                ) {
                    listener(result, skuDetails)
                }
            })
    }

    private fun launchPurchaseFlow(skuDetails: SkuDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)
        Log.i(TAG, "launchPurchaseFlow result ${responseCode}")
    }

    companion object {
        const val TAG = "InAppPurchases"
    }
}