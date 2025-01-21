package com.husnain.authy.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.core.text.layoutDirection
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.husnain.authy.R
import com.husnain.authy.databinding.ActivityMainBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.fragment.main.home.HomeFragment
import com.husnain.authy.utls.BackPressedExtensions.goBackPressed
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : LocalizationActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navHostFragment: Fragment

    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var billingClient: BillingClient

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val lang = preferenceManager.getLang()
        val locale = Locale(lang)
        Locale.setDefault(locale)
        window.decorView.layoutDirection = locale.layoutDirection
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inIt()
    }

    private fun inIt() {
        setupBillingClient()
        setUpBottomBar()
        inItAdmob()
        handleBackPressed()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.e("LOG_AUTHY", "Billing service disconnected")
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

    private fun setUpBottomBar() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.afterAuthActivityNavHostFragment)!!
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.setupWithNavController(navHostFragment.findNavController())

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->

            if (destination.id == R.id.addAccountFragment) {
                binding.mainBannerAdView.gone()
                stopShimmer()
            } else {
                binding.mainBannerAdView.visible()
            }

            if (destination.id == R.id.homeFragment || destination.id == R.id.newToolsFragment || destination.id == R.id.settingFragment) {
                binding.bottomNavigationView.visible()
            } else {
                binding.bottomNavigationView.gone()
            }
        }
    }

    private fun handleBackPressed() {
        goBackPressed {
            if (navHostFragment.childFragmentManager.fragments.first() is HomeFragment) {
                finishAffinity()
            } else {
                navHostFragment.findNavController().popBackStack()
            }
        }
    }

    fun changeLanguage(language: String) {
        setLanguage(language)
    }

    private fun inItAdmob() {
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            MobileAds.initialize(this@MainActivity) {
                Log.d(Constants.TAG, "Admob initialized successfully")
            }
        }

        val adRequest = AdRequest.Builder().build()
        binding.mainBannerAdView.loadAd(adRequest)

        binding.mainBannerAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                stopShimmer()
                binding.mainBannerAdView.visible()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                stopShimmer()
                binding.mainBannerAdView.gone()
                binding.mainBannerAdView.loadAd(adRequest)
                Log.e(Constants.TAG, "Ad failed to load: ${adError.message}")
            }
        }

    }

    private fun stopShimmer() {
        binding.adShimmer.stopShimmer()
        binding.adShimmer.gone()
    }

    private fun checkSubscriptionStatus() {
        if (!billingClient.isReady) {
            Log.e("LOG_AUTHY", "BillingClient is not ready")
            return
        }

        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val subscribedProduct = purchaseList.firstOrNull { purchase ->
                    purchase.products.contains("weekly_plan") || purchase.products.contains("monthly_plan")
                }

                if (subscribedProduct != null) {
                    Log.d("LOG_AUTHY", "Product ID = ${subscribedProduct.products}")
                    val isAutoRenewing = subscribedProduct.isAutoRenewing
                    if (isAutoRenewing) {
                        Log.d("LOG_AUTHY", "Subscription is active. autoRenewing = $isAutoRenewing")
                        preferenceManager.saveSubscriptionActive(true)
                    } else {
                        Log.d(
                            "LOG_AUTHY",
                            "Subscription is not active. autoRenewing = $isAutoRenewing"
                        )
                        preferenceManager.saveSubscriptionActive(false)
                    }
                } else {
                    Log.d("LOG_AUTHY", "No active subscriptions found")
                    preferenceManager.saveSubscriptionActive(false)
                }
            } else {
                Log.e("LOG_AUTHY", "Failed to fetch purchases: ${billingResult.debugMessage}")
                preferenceManager.saveSubscriptionActive(false)
            }
        }
    }
}