package com.husnain.authy.utls

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.husnain.authy.R

class ShimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class ShimmerType {
        SMALL, MEDIUM
    }

    private var shimmerType: ShimmerType = ShimmerType.MEDIUM // Default to medium type

    init {
        // Initialize custom view and inflate the correct layout
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShimmerView, 0, 0)
        val typeIndex = typedArray.getInt(R.styleable.ShimmerView_shimmerType, 1) // Default to MEDIUM
        shimmerType = if (typeIndex == 0) ShimmerType.SMALL else ShimmerType.MEDIUM
        typedArray.recycle()

        // Inflate the appropriate layout based on the shimmer type
        inflateShimmerLayout()
    }

    private fun inflateShimmerLayout() {
        val inflater = LayoutInflater.from(context)

        when (shimmerType) {
            ShimmerType.SMALL -> {
                // Inflate the small shimmer layout
                inflater.inflate(R.layout.native_small_shimmer, this, true)
            }
            ShimmerType.MEDIUM -> {
                // Inflate the medium shimmer layout
                inflater.inflate(R.layout.native_ad_layout_shimmer, this, true)
            }
        }
    }

    // Method to change the shimmer type dynamically
    fun setShimmerType(type: ShimmerType) {
        if (shimmerType != type) {
            shimmerType = type
            removeAllViews() // Remove any previous layout
            inflateShimmerLayout() // Inflate the new layout based on the new shimmer type
        }
    }
}
