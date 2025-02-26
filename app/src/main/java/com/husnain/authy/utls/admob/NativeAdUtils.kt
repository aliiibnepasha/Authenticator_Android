package com.husnain.authy.utls.admob

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.husnain.authy.R
import com.google.android.gms.ads.nativead.NativeAdView
import com.husnain.authy.BuildConfig

object NativeAdUtils {

    private var nativeAd: NativeAd? = null

    /**
     * Preload a Native Ad
     */
    fun loadNativeAd(context: Context, adId: String,onAdLoaded: (NativeAd?) -> Unit) {
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
     * Binds the Native Ad to the provided Ad Layout
     */
    fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the ad content
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)

        // Bind values
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

        // Bind MediaView
        adView.mediaView?.mediaContent = nativeAd.mediaContent

        // Bind Star Rating
        val starRating = nativeAd.starRating
        if (starRating != null && starRating > 0) {
            (adView.starRatingView as? RatingBar)?.apply {
                rating = starRating.toFloat()
                visibility = View.VISIBLE
            }
        } else {
            adView.starRatingView?.visibility = View.GONE
        }

        // Set the native ad
        adView.setNativeAd(nativeAd)
    }


    /**
     * Retrieves the Native Ad ID based on Debug/Release mode
     */

}
