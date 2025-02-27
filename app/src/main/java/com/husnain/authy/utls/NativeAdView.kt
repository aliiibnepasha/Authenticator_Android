package com.husnain.authy.utls

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.husnain.authy.R

class NativeAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class AdType {
        SMALL, MEDIUM
    }

    private var adType: AdType = AdType.MEDIUM // Default to medium type

    init {
        // Initialize custom view and inflate the correct layout
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NativeAdView, 0, 0)
        val typeIndex = typedArray.getInt(R.styleable.NativeAdView_adType, 1) // Default to MEDIUM
        adType = if (typeIndex == 0) AdType.SMALL else AdType.MEDIUM
        typedArray.recycle()

        // Inflate the appropriate layout based on the ad type
        inflateAdLayout()
    }

    private fun inflateAdLayout() {
        val inflater = LayoutInflater.from(context)

        when (adType) {
            AdType.SMALL -> {
                // Inflate the small ad layout
                inflater.inflate(R.layout.native_ad_view_small, this, true)
            }
            AdType.MEDIUM -> {
                // Inflate the medium ad layout
                inflater.inflate(R.layout.native_ad_layout, this, true)
            }
        }
    }

    // Method to change the ad type dynamically
    fun setAdType(type: AdType) {
        if (adType != type) {
            adType = type
            removeAllViews() // Remove any previous layout
            inflateAdLayout() // Inflate the new layout based on the new ad type
        }
    }
}
