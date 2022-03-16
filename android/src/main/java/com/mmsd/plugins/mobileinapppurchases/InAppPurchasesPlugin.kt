package com.mmsd.plugins.mobileinapppurchases

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.PluginMethod
import com.getcapacitor.PluginCall
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin

@CapacitorPlugin(name = "InAppPurchases")
class InAppPurchasesPlugin : Plugin() {

    private val implementation = InAppPurchases()

    private var inProgressPurchases: MutableList<Purchase>? = null

    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value")
        val ret = JSObject()
        ret.put("value", implementation.echo(value!!))
        call.resolve(ret)
    }

    @PluginMethod
    fun connect(call: PluginCall) {
        implementation.connect(bridge.context, bridge.activity) { result: BillingResult ->
            val jsonResponce = JSObject()
            jsonResponce.put("result", result.getJson())
            call.resolve(jsonResponce)
        }
    }

    @PluginMethod
    fun purchesConsumableProduct(call: PluginCall) {
        val productId = call.getString("productId")!!
        implementation.purchesProduct(productId, BillingClient.SkuType.INAPP) { billingResult: BillingResult, purchases: MutableList<Purchase>? ->
            inProgressPurchases = purchases
            val jsonResponce = JSObject()
            jsonResponce.put("result", billingResult.getJson())
            jsonResponce.put("purchases", purchases?.map { it.getJson() } ?: JSObject() )
            call.resolve(jsonResponce)
        }
    }

    @PluginMethod
    fun purchesNonConsumableProduct(call: PluginCall) {
        val productId = call.getString("productId")!!
        implementation.purchesProduct(productId, BillingClient.SkuType.SUBS) { billingResult: BillingResult, purchases: MutableList<Purchase>? ->
            inProgressPurchases = purchases
            val jsonResponce = JSObject()
            jsonResponce.put("result", billingResult.getJson())
            jsonResponce.put("purchases", purchases?.map { it.getJson() } ?: JSObject() )
            call.resolve(jsonResponce)
        }
    }

    @PluginMethod
    fun consumeProduct(call: PluginCall) {
        val orderId = call.getString("orderId")
        val purchase = inProgressPurchases?.filter { it.orderId.equals(orderId) }?.first()
        if (purchase != null) {
            implementation.consumeProduct(purchase) { billingResult: BillingResult, token: String ->
                val jsonResponce = JSObject()
                jsonResponce.put("result", billingResult.getJson())
                jsonResponce.put("token", token)
                call.resolve(jsonResponce)
            }
        } else {
            call.resolve(buildItemUnavailableResultJson(orderId ?: "null"))
        }
    }

    @PluginMethod
    fun acknowledgePurchase(call: PluginCall) {
        val orderId = call.getString("orderId")
        val purchase = inProgressPurchases?.filter { it.orderId.equals(orderId) }?.first()
        if (purchase != null) {
            implementation.acknowledgePurchase(purchase) { billingResult: BillingResult ->
                val jsonResponce = JSObject()
                jsonResponce.put("result", billingResult.getJson())
                call.resolve(jsonResponce)
            }
        } else {
            call.resolve(buildItemUnavailableResultJson(orderId ?: "null"))
        }
    }

    private fun buildItemUnavailableResultJson(orderId: String): JSObject {
        val billingResultBuilder = BillingResult.newBuilder()
        billingResultBuilder.setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
        billingResultBuilder.setDebugMessage("Don't exist in progress purchase with orderId: $orderId")
        val billingResult = billingResultBuilder.build()
        val jsonResponce = JSObject()
        jsonResponce.put("result", billingResult.getJson())
        return jsonResponce
    }
}

fun BillingResult.getJson(): JSObject {
    val json = JSObject()
    json.put("responseCode", responseCode)
    json.put("message", debugMessage)
    return json
}

fun Purchase.getJson(): JSObject {
    val json = JSObject()
    json.put("orderId", orderId)
    json.put("originalJson", originalJson)
    json.put("purchaseToken", purchaseToken)
    json.put("signature", signature)
    return json
}