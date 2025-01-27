package com.husnain.authy.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.core.text.layoutDirection
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
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
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : LocalizationActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var navHostFragment: Fragment
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var preferenceManager: PreferenceManager
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var adRequest: AdRequest
    private var isAdLoaded = false

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
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        setContentView(binding.root)
        inIt()
    }

    private fun inIt() {
        preloadAd()
        setUpBottomBar()
//        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleListener())
        handleBackPressed()
    }

    fun preloadAd() {
        if (!preferenceManager.isSubscriptionActive() && NetworkUtils.isNetworkAvailable(this)) {
            // Initially hide the ad view

            adRequest = AdRequest.Builder().build()
            binding.mainBannerAdView.loadAd(adRequest)
            binding.mainBannerAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isAdLoaded = true
                    val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                    if (currentFragment !is SubscriptionFragment && currentFragment !is LocalizeFragment) {
                        binding.mainBannerAdView.visible()
                    } else {
                        binding.mainBannerAdView.gone()
                    }
                    Log.d(Constants.TAG, "Ad loaded successfully")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoaded = false
                    binding.mainBannerAdView.gone()
                    Log.d(Constants.TAG, "Ad failed to load: ${adError.message}")
                }
            }
        } else {
            isAdLoaded = false
            binding.mainBannerAdView.gone()
        }
    }

    private fun setUpBottomBar() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.afterAuthActivityNavHostFragment)!!
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.setupWithNavController(navHostFragment.findNavController())

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.newToolsFragment, R.id.settingFragment -> {
                    if (isAdLoaded) {
                        binding.mainBannerAdView.visible()
                    }
                    binding.bottomNavigationView.visible()
                }

                else -> {
                    binding.mainBannerAdView.gone()
                    binding.bottomNavigationView.gone()
                }
            }
        }
    }

    private fun handleBackPressed() {
        goBackPressed {
            if (navHostFragment.childFragmentManager.fragments.first() is HomeFragment) {
                finishAffinity()
            } else if (navHostFragment.childFragmentManager.fragments.first() is SubscriptionFragment) {

            } else {
                navHostFragment.findNavController().popBackStack()
            }
        }
    }

    fun changeLanguage(language: String) {
        setLanguage(language)
    }

    override fun onResume() {
        super.onResume()
    }

//    inner class AppLifecycleListener : DefaultLifecycleObserver {
//        override fun onStart(owner: LifecycleOwner) {
//            super.onStart(owner)
//            if (navHostFragment.isAdded && navHostFragment.childFragmentManager.fragments.isNotEmpty()) {
//                if (!preferenceManager.isSubscriptionActive() && navHostFragment.childFragmentManager.fragments.first() !is SubscriptionFragment) {
//                    (application as App).appOpenAdManager.showAdIfAvailableFromFragment(this@MainActivity) {}
//                }
//            }
//
//            Log.d(Constants.TAG, "foreground")
//        }
//
//        override fun onStop(owner: LifecycleOwner) {
//            super.onStop(owner)
//            isAppComingToForeground = false
//            Log.d(Constants.TAG, "background")
//        }
//    }
}
