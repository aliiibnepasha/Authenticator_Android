package com.husnain.authy.ui.fragment.main.recentlyDeleted

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelTotp
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.databinding.ItemRecentlyDeletedBinding
import com.husnain.authy.utls.TotpUtil

class AdapterRecentlyDeleted(
    private var items: List<RecentlyDeleted>,
) :
    RecyclerView.Adapter<AdapterRecentlyDeleted.ViewHolder>() {
    private var callBack: (RecentlyDeleted) -> Unit = {}
    private var selectedPosition = -1;

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentlyDeletedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }


    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        holder.bind(data)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.stopUpdates()
    }

    fun setOnClickListener(callback: (RecentlyDeleted) -> Unit) {
        this.callBack = callback
    }

    inner class ViewHolder(val binding: ItemRecentlyDeletedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var updateHandler: Handler? = null

        fun bind(data: RecentlyDeleted) {
            binding.imgLogo.setImageResource(R.drawable.img_baby_brain)
            binding.tvServiceName.text = data.name

            if (bindingAdapterPosition == selectedPosition) {
                binding.imgRadioButton.setImageResource(R.drawable.ic_radio_selected)
            } else {
                binding.imgRadioButton.setImageResource(R.drawable.ic_radio_unselected)
            }

            binding.root.setOnClickListener {
                val previousSelectedPosition = selectedPosition
                selectedPosition = bindingAdapterPosition

                notifyItemChanged(previousSelectedPosition)
                notifyItemChanged(selectedPosition)

                callBack.invoke(data)
            }

            updateHandler = Handler(Looper.getMainLooper())
            val updateTotp = object : Runnable {
                override fun run() {
                    try {
                        val totp = TotpUtil.generateTotp(data.secret)
                        binding.tvTotp.text = totp.chunked(3).joinToString(" ")

                        val remainingSeconds = TotpUtil.getRemainingSeconds()
                        binding.tvCounter.text = remainingSeconds.toString()

                        updateHandler?.postDelayed(this, 1000L)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        binding.tvTotp.text = itemView.context.getString(R.string.string_error)
                    }
                }
            }

            // Start TOTP updates
            updateHandler?.post(updateTotp)
        }

        fun stopUpdates() {
            updateHandler?.removeCallbacksAndMessages(null)
        }
    }
}