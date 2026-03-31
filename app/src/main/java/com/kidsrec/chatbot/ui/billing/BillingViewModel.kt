package com.kidsrec.chatbot.ui.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.kidsrec.chatbot.data.repository.BillingManager
import com.kidsrec.chatbot.data.repository.PurchaseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val products: StateFlow<List<ProductDetails>> = billingManager.products
    val purchaseState: StateFlow<PurchaseState> = billingManager.purchaseState

    init {
        billingManager.initialize()
    }

    fun purchasePremium(activity: Activity) {
        val product = products.value.firstOrNull() ?: return
        billingManager.launchPurchaseFlow(activity, product)
    }

    fun resetPurchaseState() {
        billingManager.resetPurchaseState()
    }
}
