package com.husnain.authy.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowMetrics
import androidx.core.text.layoutDirection
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.databinding.ActivityAuthBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.fragment.auth.SigninFragment
import com.husnain.authy.ui.fragment.auth.SignupFragment
import com.husnain.authy.ui.fragment.main.subscription.SubscriptionFragment
import com.husnain.authy.utls.BackPressedExtensions.goBackPressed
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.Flags
import com.husnain.authy.utls.NetworkUtils
import com.husnain.authy.utls.admob.AdUtils
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : LocalizationActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var billingClient: BillingClient
    private lateinit var navController: NavController
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var navHostFragment: Fragment
    private lateinit var adView: AdView
    private var isAdLoaded = false

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var adRequest: AdRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        adRequest = AdRequest.Builder().build()
        inIt()
    }

    private fun inIt() {
        inItAdmob()
        setupBillingClient()
        setupNavController()
        handleBackPressed()
    }

    private fun setupNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.auth_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.signinFragment || destination.id == R.id.signupFragment || destination.id == R.id.forgotPasswordFragment) {
                showNavigationBar()
            } else {
                hideNavigationBar()
            }
            if (destination.id == R.id.onboardingFragment) {
                setStatusBarColor(R.color.colorPrimary)
                if (isAdLoaded) {
                    binding.adContainer.visible()
                }
            } else if (destination.id == R.id.splashFragment) {
                setStatusBarColor(R.color.colorPrimary)
            } else {
                setStatusBarColor(R.color.white)
                binding.adContainer.gone()
            }
        }
    }

    private fun hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun showNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.insetsController?.show(WindowInsets.Type.navigationBars())
        }
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
                    preferenceManager.saveLifeTimeAccessActive(true)
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
                    purchase.products.contains(Constants.weaklySubId) || purchase.products.contains(
                        Constants.monthlySubId
                    )
                }

                if (subscribedProduct != null) {
                    // Check if the subscription is active and auto-renewing
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
                Log.e("LOG_AUTHY", "Failed to fetch SUBS purchases: ${billingResult.debugMessage}")
                preferenceManager.saveSubscriptionActive(false)
            }
        }
    }

    private fun inItAdmob() {
        if (!preferenceManager.isSubscriptionActive()) {
            //make and set the adView
            adView = AdView(this);
            adView.adUnitId = AdUtils.getBannerAdId(this);
            adView.setAdSize(adSize);
            binding.adContainer.removeAllViews()
            binding.adContainer.addView(adView)

            //Request ad
            adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isAdLoaded = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoaded = false
                    binding.adContainer.gone()
                    Log.d(Constants.TAG, adError.message)
                }
            }
        } else {
            isAdLoaded = false
            binding.adContainer.gone()
        }
    }


    private fun handleBackPressed() {
        navHostFragment = supportFragmentManager.findFragmentById(R.id.auth_nav_host_fragment)!!
        goBackPressed {
            val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
            when (currentFragment) {
                is SubscriptionFragment -> {}
                is SigninFragment -> {
                    startMainActivityFromGuestToLogin()
                }

                is SignupFragment -> {
                    startMainActivityFromGuestToLogin()
                }

                else -> {
                    navHostFragment.findNavController().popBackStack()
                }
            }
        }
    }

    private fun startMainActivityFromGuestToLogin() {
        Constants.isComingToAuthFromGuestToSignIn = false
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        finish()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val lang = preferenceManager.getLang()
        val locale = Locale(lang)
        Locale.setDefault(locale)
        window.decorView.layoutDirection = locale.layoutDirection
    }

    private fun setStatusBarColor(colorResId: Int) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = getColor(colorResId)
    }

    override fun onResume() {
        super.onResume()
        //this flag is being set to true in AdUtils.kt when ad is called for show
        if (Flags.isComingFromInterstitialAdClose) {
            Flags.isComingFromInterstitialAdClose = false
            onInterstitialAdClosed()
        }
    }

    private fun onInterstitialAdClosed() {
        //As the subscription fragment only come on auth activity when user is onboarding first time
        //so we now this only going to happen in above stated way we are navigation to onboarding fragment
        navHostFragment.findNavController().navigate(R.id.action_subscriptionFragment2_to_onboardingFragment)
    }

    private val adSize: AdSize
        get() {
            val displayMetrics = resources.displayMetrics
            val adWidthPixels =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val windowMetrics: WindowMetrics = this.windowManager.currentWindowMetrics
                    windowMetrics.bounds.width()
                } else {
                    displayMetrics.widthPixels
                }
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }
}