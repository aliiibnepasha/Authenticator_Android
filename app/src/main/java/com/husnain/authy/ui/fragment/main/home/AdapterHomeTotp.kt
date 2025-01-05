package com.husnain.authy.ui.fragment.main.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.husnain.authy.R
import com.husnain.authy.data.ModelTotp
import com.husnain.authy.data.room.EntityTotp
import com.husnain.authy.databinding.ItemHomeTotpBinding
import com.husnain.authy.utls.TotpUtil
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator

class AdapterHomeTotp(private val items: List<ModelTotp>,private val callBack:(ModelTotp) -> Unit) :
    RecyclerView.Adapter<AdapterHomeTotp.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemHomeTotpBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
                        binding.tvTotp.text = totp

                        val remainingSeconds = TotpUtil.getRemainingSeconds()
                        binding.tvCounter.text = remainingSeconds.toString()

                        updateHandler?.postDelayed(this, 1000L)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        binding.tvTotp.text = "Error"
                    }
                }
            }

            // Start TOTP updates
            updateHandler?.post(updateTotp)

            // Handle copy action
            binding.imgCopy.setOnClickListener {
                val clipboard = it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("TOTP", binding.tvTotp.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(it.context, "Otp copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        fun stopUpdates() {
            updateHandler?.removeCallbacksAndMessages(null)
        }
    }


}