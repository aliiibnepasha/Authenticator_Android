package com.husnain.authy.ui.fragment.main.home

import android.annotation.SuppressLint
import android.graphics.ColorSpace.Model
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import androidx.recyclerview.widget.RecyclerView
import com.husnain.authy.R
import com.husnain.authy.data.ModelTotp
import com.husnain.authy.databinding.ItemHomeTotpBinding
import com.husnain.authy.utls.SearchFilter
import com.husnain.authy.utls.TotpUtil
import com.husnain.authy.utls.copyToClip
import com.husnain.authy.utls.showSnackBar

class AdapterHomeTotp(private var items: List<ModelTotp>, private val callBack:(ModelTotp) -> Unit) :
    RecyclerView.Adapter<AdapterHomeTotp.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemHomeTotpBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    private var originalItems: List<ModelTotp> = ArrayList(items)

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


    fun getFilter(): Filter {
        return SearchFilter(originalList = originalItems, adapter = this)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<ModelTotp>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemHomeTotpBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var updateHandler: Handler? = null

        fun bind(data: ModelTotp) {
            binding.imgLogo.setImageResource(R.drawable.img_baby_brain)
            binding.tvServiceName.text = data.serviceName


            updateHandler = Handler(Looper.getMainLooper())
            val updateTotp = object : Runnable {
                override fun run() {
                    try {
                        val totp = TotpUtil.generateTotp(data.secretKey)
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

            binding.imgCopy.setOnClickListener {
                it.context.copyToClip(binding.tvTotp.text.toString())
                showSnackBar(binding.root, "Code copied to clipboard.")
            }
        }

        fun stopUpdates() {
            updateHandler?.removeCallbacksAndMessages(null)
        }
    }
}