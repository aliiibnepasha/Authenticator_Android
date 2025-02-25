package com.husnain.authy.utls.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.datatransport.runtime.scheduling.jobscheduling.SchedulerConfig.Flag
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.husnain.authy.BuildConfig
import com.husnain.authy.R
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.Flags

object AdUtils {
    private var interstitialAd: InterstitialAd? = null
    private var isDebug = BuildConfig.DEBUG
//    private var isDebug = false

    /**
     * Preload the interstitial ad and notify the loading status.
     *
     * @param activity The activity instance
     * @param onAdLoadStatus A callback with a boolean indicating whether the ad is loaded
     */
    fun loadInterstitialAd(activity: Activity, onAdLoadStatus: (isLoaded: Boolean) -> Unit) {
        InterstitialAd.load(
            activity,
            getInterstitialAdId(activity),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(Constants.TAG, "interstial ad on success")
                    interstitialAd = ad
                    onAdLoadStatus(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(Constants.TAG, "interstial ad fail = ${loadAdError.message}")
                    interstitialAd = null
                    onAdLoadStatus(false)
                }
            }
        )
    }

    /**
     * Shows the interstitial ad and sets a flag when the ad is shown or fails to load.
     * The flag (`Flags.isComingFromInterstitialAdClose`) is set to `true` when the ad is either shown or not available,
     * indicating the app is resuming after the ad.
     */

    fun showInterstitialAdWithCallback(activity: Activity,failureShowCallback:() -> Unit) {
        if (isDebug) {
            failureShowCallback()
            return
        }
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Flags.isInterstitialAdShowing = false
                    interstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Flags.isInterstitialAdShowing = false
                    interstitialAd = null
                    Flags.isComingFromInterstitialAdClose = false
                    failureShowCallback.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    Flags.isInterstitialAdShowing = true
                    Flags.isComingFromInterstitialAdClose = true
                    failureShowCallback()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Flags.isComingFromInterstitialAdClose = false
            failureShowCallback.invoke()
        }
    }

    private fun getInterstitialAdId(context: Context): String {
        return if (isDebug) {
            context.getString(R.string.admob_interstitial_ad_id_test)
        } else {
            context.getString(R.string.admob_interstitial_ad_id_release)
        }
    }

    fun getBannerAdId(context: Context): String {
        return if (isDebug) {
            context.getString(R.string.admob_banner_id_test)
        } else {
            context.getString(R.string.admob_banner_id_release)
        }
    }

    fun getAppOpenAdId(context: Context): String {
        return if (isDebug) {
            context.getString(R.string.admob_app_open_ad_unit_id_test)
        } else {
            context.getString(R.string.admob_app_open_ad_unit_id_release)
        }
    }
}