package com.husnain.authy.utls.admob

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.husnain.authy.BuildConfig
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.Flags
import java.util.concurrent.TimeUnit

class AppOpenAdManager(private val application: Application) {
    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private val adTimeout = TimeUnit.HOURS.toMillis(4)
    private var loadTime: Long = 0

    fun loadAd() {
        if (isAdAvailable()) return

        AppOpenAd.load(
            application,
            AdUtils.getAppOpenAdId(application),
            AdRequest.Builder().build(),
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(Constants.TAG,"app open ad error = ${loadAdError.message}")
                }
            }
        )
    }

    fun showAdIfAvailableFromFragment(activity: Activity, onAdComplete: () -> Unit) {
//        if (BuildConfig.DEBUG){
//            onAdComplete()
//            return
//        }
        if (Flags.isInterstitialAdShowing){
            return
        }

        if (isShowingAd || !isAdAvailable()) {
            onAdComplete()
            return
        }

        appOpenAd?.let { ad ->
            isShowingAd = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    onAdComplete()

                    // Reload the ad after it has been dismissed
                    loadAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingAd = false
                    onAdComplete()
                }

                override fun onAdShowedFullScreenContent() {
                }
            }

            ad.show(activity)
        } ?: run {
            onAdComplete()  // If ad is null, proceed to the next screen immediately
        }
    }


    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && System.currentTimeMillis() - loadTime < adTimeout
    }
}