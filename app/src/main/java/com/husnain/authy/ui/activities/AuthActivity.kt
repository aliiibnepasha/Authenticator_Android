package com.husnain.authy.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.core.text.layoutDirection
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.husnain.authy.databinding.ActivityAuthBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : LocalizationActivity() {
    private lateinit var binding: ActivityAuthBinding
    @Inject lateinit var preferenceManager: PreferenceManager
    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inIt()
    }

    private fun inIt() {
        setupBillingClient()
        setOnClickListener()
    }

    private fun setOnClickListener() {

    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkSubscriptionStatus()
                } else {
                    Log.e(
                        "LOG_AUTHY",
                        "Error setting up BillingClient: ${billingResult.debugMessage}"
                    )
                }
            }
        })
    }
    private fun checkSubscriptionStatus() {
        if (!billingClient.isReady) {
            Log.e("LOG_AUTHY", "BillingClient is not ready")
            return
        }

        // First, check for INAPP purchases (lifetime or one-time purchases)
        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(Constants.TAG, purchaseList.toString())

                val inAppPurchase = purchaseList.firstOrNull { purchase ->
                    purchase.products.contains(Constants.lifeTimePorductId)
                }

                if (inAppPurchase != null) {
                    // Lifetime purchase is always considered active
                    Log.d("LOG_AUTHY", "Lifetime purchase is active")
                    preferenceManager.saveSubscriptionActive(true)
                } else {
                    // No lifetime purchase, check for subscriptions
                    checkForSubscription()
                }
            } else {
                Log.e("LOG_AUTHY", "Failed to fetch INAPP purchases: ${billingResult.debugMessage}")
                checkForSubscription()  // Move to subscription check if INAPP fails
            }
        }
    }

    private fun checkForSubscription() {
        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(Constants.TAG, purchaseList.toString())

                val subscribedProduct = purchaseList.firstOrNull { purchase ->
                    purchase.products.contains(Constants.weaklySubId) || purchase.products.contains(Constants.monthlySubId)
                }

                if (subscribedProduct != null) {
                    // Check if the subscription is active and auto-renewing
                    val isAutoRenewing = subscribedProduct.isAutoRenewing
                    if (isAutoRenewing) {
                        Log.d("LOG_AUTHY", "Subscription is active. autoRenewing = $isAutoRenewing")
                        preferenceManager.saveSubscriptionActive(true)
                    } else {
                        Log.d("LOG_AUTHY", "Subscription is not active. autoRenewing = $isAutoRenewing")
                        preferenceManager.saveSubscriptionActive(false)
                    }
                } else {
                    Log.d("LOG_AUTHY", "No active subscriptions found")
                    preferenceManager.saveSubscriptionActive(false)
                }
            } else {
                Log.e("LOG_AUTHY", "Failed to fetch SUBS purchases: ${billingResult.debugMessage}")
                preferenceManager.saveSubscriptionActive(false)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val lang = preferenceManager.getLang()
        val locale = Locale(lang)
        Locale.setDefault(locale)
        window.decorView.layoutDirection = locale.layoutDirection
    }
}