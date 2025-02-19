package com.husnain.authy.ui.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowMetrics
import androidx.core.text.layoutDirection
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.app.App
import com.husnain.authy.databinding.ActivityMainBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.fragment.main.home.HomeFragment
import com.husnain.authy.ui.fragment.main.localization.LocalizeFragment
import com.husnain.authy.ui.fragment.main.subscription.SubscriptionFragment
import com.husnain.authy.utls.BackPressedExtensions.goBackPressed
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.NetworkUtils
import com.husnain.authy.utls.admob.AdUtils
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : LocalizationActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var navHostFragment: Fragment
    @Inject
    lateinit var auth: FirebaseAuth
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var adRequest: AdRequest
    private var isAdLoaded = false
    private lateinit var adView: AdView

    override fun onStart() {
        super.onStart()
        (application as App).registerMainActivity(this)
    }

    override fun onStop() {
        super.onStop()
        (application as App).unregisterMainActivity()
    }

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
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        //other code
        inIt()
    }

    private fun inIt() {
        preloadAd()
        setUpBottomBar()
        handleBackPressed()
    }

    fun preloadAd() {
        if (!preferenceManager.isSubscriptionActive() && NetworkUtils.isNetworkAvailable(this)) {
            //make and set the adView
            adView = AdView(this);
            adView.adUnitId = AdUtils.getBannerAdId(this);
            adView.setAdSize(adSize);
            binding.addViewLayout.removeAllViews()
            binding.addViewLayout.addView(adView)

            //Request ad
            adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isAdLoaded = true
                    val currentFragment =
                        navHostFragment.childFragmentManager.fragments.firstOrNull()
                    if (currentFragment !is SubscriptionFragment && currentFragment !is LocalizeFragment) {
                        binding.addViewLayout.visible()
                    } else {
                        binding.addViewLayout.gone()
                    }
                    Log.d(Constants.TAG, "Ad loaded successfully")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoaded = false
                    binding.addViewLayout.gone()
                    Log.d(Constants.TAG, "Ad failed to load: ${adError.message}")
                }
            }
        } else {
            isAdLoaded = false
            binding.addViewLayout.gone()
        }
    }

    private fun setUpBottomBar() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.afterAuthActivityNavHostFragment)!!
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.setupWithNavController(navHostFragment.findNavController())

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.subscriptionFragment) {
                hideNavigationBar()
            } else {
                showNavigationBar()
            }

            if (destination.id == R.id.homeFragment) {
                setStatusBarColor(R.color.colorPrimary)
            } else {
                setStatusBarColor(R.color.white)
            }

            when (destination.id) {
                R.id.homeFragment, R.id.newToolsFragment, R.id.settingFragment -> {
                    if (isAdLoaded) {
                        binding.addViewLayout.visible()
                    }
                    binding.bottomNavigationView.visible()
                }

                else -> {
                    binding.addViewLayout.gone()
                    binding.bottomNavigationView.gone()
                }
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

    private fun handleBackPressed() {
        goBackPressed {
            if (navHostFragment.childFragmentManager.fragments.first() is HomeFragment) {
                moveTaskToBack(true)
            } else if (navHostFragment.childFragmentManager.fragments.first() is SubscriptionFragment) {

            } else {
                navHostFragment.findNavController().popBackStack()
            }
        }
    }

    fun changeLanguage(language: String) {
        setLanguage(language)
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

    private fun setStatusBarColor(colorResId: Int) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = getColor(colorResId)
    }
}
