package com.husnain.authy.utls.admob
import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdUtils {
    private var interstitialAd: InterstitialAd? = null

    // Preload the ad
    fun loadInterstitialAd(activity: Activity, adUnitId: String) {
        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    // Show the ad with a callback
    fun showInterstitialAdWithCallback(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdDismissed() // Called after the ad is dismissed
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    onAdDismissed() // Called if the ad failed to show
                }

                override fun onAdShowedFullScreenContent() {
                    // Handle additional logic if needed when the ad is shown
                }
            }
            interstitialAd?.show(activity)
        } else {
            // If the ad wasn't loaded, immediately call the callback
            onAdDismissed()
        }
    }
}