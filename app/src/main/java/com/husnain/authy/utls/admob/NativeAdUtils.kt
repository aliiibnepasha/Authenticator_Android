package com.husnain.authy.utls.admob

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.husnain.authy.R

object NativeAdUtils {

    private var nativeAd: NativeAd? = null

    /**
     * Preload a Native Ad with or without media
     */
    fun preloadNativeAd(context: Context, adId: String) {
        loadNativeAd(context, adId) { ad ->
            nativeAd = ad // Store the ad for later use
        }
    }

    /**
     * Load a Native Ad and return immediately via callback
     */
    fun loadNativeAd(context: Context, adId: String, onAdLoaded: (NativeAd?) -> Unit) {
        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { ad ->
                nativeAd = ad
                onAdLoaded(ad)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd = null
                    onAdLoaded(null)
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * Get a preloaded Ad if available, otherwise load a new one
     */
    fun getOrLoadNativeAd(context: Context, adId: String, onAdAvailable: (NativeAd?) -> Unit) {
        if (nativeAd != null) {
            onAdAvailable(nativeAd)
        } else {
            loadNativeAd(context, adId, onAdAvailable)
        }
    }

    /**
     * Binds a Native Ad to a NativeAdView
     */
    fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView, isMedia: Boolean) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

        (adView.headlineView as? TextView)?.text = nativeAd.headline
        (adView.bodyView as? TextView)?.text = nativeAd.body
        (adView.callToActionView as? Button)?.apply {
            text = nativeAd.callToAction
            visibility = if (nativeAd.callToAction != null) View.VISIBLE else View.GONE
        }

        // Bind Icon
        nativeAd.icon?.let {
            (adView.iconView as? ImageView)?.setImageDrawable(it.drawable)
            adView.iconView?.visibility = View.VISIBLE
        } ?: run {
            adView.iconView?.visibility = View.GONE
        }

        // Conditional MediaView Binding
        if (isMedia) {
            // Bind MediaView if media is requested
            if (nativeAd.mediaContent != null) {
                adView.mediaView = adView.findViewById(R.id.ad_media)
                adView.mediaView?.mediaContent = nativeAd.mediaContent
                adView.mediaView?.visibility = View.VISIBLE
            } else {
                adView.mediaView?.visibility = View.GONE
            }
        }

        // Set the native ad
        adView.setNativeAd(nativeAd)
    }

}
