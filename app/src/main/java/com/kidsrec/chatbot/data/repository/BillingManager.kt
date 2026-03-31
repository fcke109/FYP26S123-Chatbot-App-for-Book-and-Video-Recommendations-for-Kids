package com.kidsrec.chatbot.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val functions: FirebaseFunctions
) : PurchasesUpdatedListener {

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_monthly"
    }

    private var billingClient: BillingClient? = null

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing service disconnected")
            }
        })
    }

    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = productDetailsList
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Cancelled
            }
            else -> {
                _purchaseState.value = PurchaseState.Error("Purchase failed")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        _purchaseState.value = PurchaseState.Processing

        // Acknowledge the purchase
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Verify purchase on server and upgrade plan
                verifyPurchaseOnServer(purchase)
            } else {
                _purchaseState.value = PurchaseState.Error("Failed to acknowledge purchase")
            }
        }
    }

    private fun verifyPurchaseOnServer(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: PREMIUM_PRODUCT_ID

        functions.getHttpsCallable("verifyPurchase")
            .call(
                mapOf(
                    "purchaseToken" to purchase.purchaseToken,
                    "productId" to productId
                )
            )
            .addOnSuccessListener {
                _purchaseState.value = PurchaseState.Success
            }
            .addOnFailureListener { e ->
                Log.e("BillingManager", "Failed to verify purchase", e)
                _purchaseState.value = PurchaseState.Error("Verification failed")
            }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }
}

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Processing : PurchaseState()
    object Success : PurchaseState()
    object Cancelled : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}
