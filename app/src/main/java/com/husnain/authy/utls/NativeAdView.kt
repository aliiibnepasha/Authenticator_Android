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
    private var isBanner: Boolean = false

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.NativeAdView, 0, 0)
            adType = if (typedArray.getInt(R.styleable.NativeAdView_adType, 1) == 0) AdType.SMALL else AdType.MEDIUM
            isBanner = typedArray.getBoolean(R.styleable.NativeAdView_isBanner, false)
            typedArray.recycle()
        }

        inflateAdLayout()
    }

    private fun inflateAdLayout() {
        val inflater = LayoutInflater.from(context)
        removeAllViews()

        if (isBanner) {
            inflater.inflate(R.layout.banner_ad_layout, this, true)
        } else {
            when (adType) {
                AdType.SMALL -> inflater.inflate(R.layout.native_ad_view_small, this, true)
                AdType.MEDIUM -> inflater.inflate(R.layout.native_ad_layout, this, true)
            }
        }
    }

    fun setAdType(type: AdType) {
        if (adType != type) {
            adType = type
            inflateAdLayout()
        }
    }

    fun setBanner(banner: Boolean) {
        if (isBanner != banner) {
            isBanner = banner
            inflateAdLayout()
        }
    }
}