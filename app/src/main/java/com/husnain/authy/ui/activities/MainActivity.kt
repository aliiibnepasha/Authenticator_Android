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
import com.husnain.authy.utls.NetworkUtils
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : LocalizationActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navHostFragment: Fragment

    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var billingClient: BillingClient
    private lateinit var adRequest: AdRequest

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
        MobileAds.initialize(this@MainActivity)
        setContentView(binding.root)
        inIt()
    }

    private fun inIt() {
        setupBillingClient()
        inItAdmob()
        setUpBottomBar()
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

    private fun inItAdmob() {
        adRequest = AdRequest.Builder().build()

        if (NetworkUtils.isNetworkAvailable(this)){
            binding.mainBannerAdView.loadAd(adRequest)
        }else{
            stopShimmer()
            binding.mainBannerAdView.gone()
            return
        }

        binding.mainBannerAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                stopShimmer()
                binding.mainBannerAdView.visible()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                stopShimmer()
                binding.mainBannerAdView.gone()
            }
        }

    }

    private fun setUpBottomBar() {
        navHostFragment = supportFragmentManager.findFragmentById(R.id.afterAuthActivityNavHostFragment)!!
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.setupWithNavController(navHostFragment.findNavController())

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.addAccountFragment || destination.id == R.id.webViewFragment) {
                stopShimmer()
                binding.mainBannerAdView.gone()
            } else {
                inItAdmob()
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

    private fun stopShimmer() {
        binding.adShimmer.stopShimmer()
        binding.adShimmer.gone()
    }

    fun changeLanguage(language: String) {
        setLanguage(language)
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
                    purchase.products.contains(Constants.weaklySubId) || purchase.products.contains(
                        Constants.monthlySubId
                    )
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