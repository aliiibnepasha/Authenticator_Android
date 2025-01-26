package com.husnain.authy.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.text.layoutDirection
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.app.App
import com.husnain.authy.databinding.ActivityMainBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.fragment.main.home.HomeFragment
import com.husnain.authy.utls.BackPressedExtensions.goBackPressed
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.Flags
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
    lateinit var auth: FirebaseAuth
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var adRequest: AdRequest
    private val vmMain: VmMain by viewModels()


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
        inItAdmob()
        setUpBottomBar()
        handleBackPressed()
    }

    private fun inItAdmob() {
        if (!preferenceManager.isSubscriptionActive()) {
            adRequest = AdRequest.Builder().build()

            if (NetworkUtils.isNetworkAvailable(this)) {
                binding.mainBannerAdView.loadAd(adRequest)
            } else {
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
                    Log.d(Constants.TAG,adError.message)
                    stopShimmer()
                    binding.mainBannerAdView.gone()
                }
            }
        }else{
            stopShimmer()
            binding.mainBannerAdView.gone()
        }
    }

    private fun setUpBottomBar() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.afterAuthActivityNavHostFragment)!!
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



    override fun onResume() {
        super.onResume()
        if (!preferenceManager.isSubscriptionActive()) {
            if (!Flags.isComingBackFromAuth) {
                (application as App).appOpenAdManager.showAdIfAvailableFromFragment(this) {}
            }
        }
        Flags.isComingBackFromAuth = false
    }
}