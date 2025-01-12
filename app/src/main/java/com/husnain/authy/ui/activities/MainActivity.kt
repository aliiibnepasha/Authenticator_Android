package com.husnain.authy.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.layoutDirection
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.husnain.authy.R
import com.husnain.authy.databinding.ActivityMainBinding
import com.husnain.authy.ui.fragment.main.home.HomeFragment
import com.husnain.authy.utls.BackPressedExtensions.goBackPressed
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import java.util.Locale

@AndroidEntryPoint
class MainActivity : LocalizationActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navHostFragment: Fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inIt()
    }

    private fun inIt() {
        setUpBottomBar()
        inItAdmob()
        handleBackPressed()
    }

    private fun setUpBottomBar() {
        navHostFragment = supportFragmentManager.findFragmentById(R.id.afterAuthActivityNavHostFragment)!!
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.setupWithNavController(navHostFragment.findNavController())

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
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

    fun changeLanguage(language: String){
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
                binding.adShimmer.stopShimmer()
                binding.adShimmer.gone()
                binding.mainBannerAdView.visible()
                Log.d(Constants.TAG, "Ad loaded successfully")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                binding.adShimmer.stopShimmer()
                binding.adShimmer.gone()
//                binding.mainBannerAdView.gone()
                Log.e(Constants.TAG, "Ad failed to load: ${adError.message}")
            }
        }

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.decorView.layoutDirection = Locale.getDefault().layoutDirection
    }
}