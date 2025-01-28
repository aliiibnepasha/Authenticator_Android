package com.husnain.authy.utls.admob
import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.husnain.authy.R
import com.husnain.authy.utls.Constants

object AdUtils {
    private var interstitialAd: InterstitialAd? = null

    /**
     * Preload the interstitial ad and notify the loading status.
     *
     * @param activity The activity instance
     * @param onAdLoadStatus A callback with a boolean indicating whether the ad is loaded
     */
    fun loadInterstitialAd(activity: Activity, onAdLoadStatus: (isLoaded: Boolean) -> Unit) {
        InterstitialAd.load(
            activity,
            activity.resources.getString(R.string.admob_interstitial_ad_id_release),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    onAdLoadStatus(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(Constants.TAG,"interstial ad fail = ${loadAdError.message}")
                    interstitialAd = null
                    onAdLoadStatus(false)
                }
            }
        )
    }

    /**
     * Show the interstitial ad with a callback for dismissal and errors.
     *
     * @param activity The activity instance
     * @param onAdDismissed A callback executed when the ad is dismissed or fails to show
     */
    fun showInterstitialAdWithCallback(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdDismissed()
        }
    }
}