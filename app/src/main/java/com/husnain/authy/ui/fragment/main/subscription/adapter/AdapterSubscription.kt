package com.husnain.authy.ui.fragment.main.subscription.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelSubscription
import com.husnain.authy.databinding.ItemSubscriptionBinding

class AdapterSubscription(private var items: List<ModelSubscription>,private val onItemSelected: (ModelSubscription) -> Unit) :
    RecyclerView.Adapter<AdapterSubscription.ViewHolder>() {
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):ViewHolder {
        val binding = ItemSubscriptionBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateData(newData: List<ModelSubscription>) {
        items = newData
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        holder.bind(data)
    }
    inner class ViewHolder(val binding: ItemSubscriptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: ModelSubscription){
            binding.tvDuration.text = data.duration.uppercase()
            binding.tvPrice.text = data.price
            binding.tvLabel.text = data.label.uppercase()
            binding.tvDuration2.text = "/${data.duration.uppercase()}"

            if (bindingAdapterPosition == selectedPosition) {
                binding.imgCircle.setImageResource(R.drawable.ic_circle_selected)
                binding.lySubscription.setBackgroundResource(R.drawable.bg_subscription_ly_with_stroke)
            } else {
                binding.imgCircle.setImageResource(R.drawable.ic_cirlce_un_selected)
                binding.lySubscription.setBackgroundResource(R.drawable.bg_subscription_ly)
            }

            binding.lySubscription.setOnClickListener {
                val previousSelectedPosition = selectedPosition
                selectedPosition = bindingAdapterPosition

                notifyItemChanged(previousSelectedPosition)
                notifyItemChanged(selectedPosition)

                onItemSelected.invoke(data)
            }
        }
    }

}