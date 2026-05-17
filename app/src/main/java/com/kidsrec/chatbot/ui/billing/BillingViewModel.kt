package com.kidsrec.chatbot.ui.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.kidsrec.chatbot.data.repository.BillingManager
import com.kidsrec.chatbot.data.repository.PurchaseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

// ViewModel for handling premium purchase screen logic
@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    // List of available premium products from Google Play Billing
    val products: StateFlow<List<ProductDetails>> = billingManager.products
    // Current purchase status such as loading, success, or error
    val purchaseState: StateFlow<PurchaseState> = billingManager.purchaseState

    init {
        // Start billing setup when ViewModel is created
        billingManager.initialize()
    }

    // Starts the premium purchase flow
    fun purchasePremium(activity: Activity) {
        val product = products.value.firstOrNull() ?: return
        billingManager.launchPurchaseFlow(activity, product)
    }

    // Clears purchase state after it has been handled by the UI
    fun resetPurchaseState() {
        billingManager.resetPurchaseState()
    }
}
