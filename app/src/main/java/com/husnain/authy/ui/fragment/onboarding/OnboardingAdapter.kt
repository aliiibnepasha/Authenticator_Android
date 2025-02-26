package com.husnain.authy.ui.fragment.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.husnain.authy.R

class OnboardingAdapter(
    private val images: List<Int>,
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.imageView.setImageResource(images[position])

        if (position == 0) {
            holder.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            val paddingInPixels = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.padding_top_20dp)
            holder.imageView.setPadding(0, paddingInPixels, 0, 0) // Add padding to the top only
        } else {
            holder.imageView.scaleType = ImageView.ScaleType.FIT_XY
            holder.imageView.setPadding(0, 0, 0, 0) // Remove any padding
        }
    }

    override fun getItemCount(): Int = images.size
}
