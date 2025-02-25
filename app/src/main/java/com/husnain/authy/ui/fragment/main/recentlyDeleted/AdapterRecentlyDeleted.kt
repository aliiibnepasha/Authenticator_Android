package com.husnain.authy.ui.fragment.main.recentlyDeleted

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.husnain.authy.R
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.databinding.ItemRecentlyDeletedBinding
import com.husnain.authy.utls.TotpUtil

class AdapterRecentlyDeleted(
    private var items: List<RecentlyDeleted>,
) : RecyclerView.Adapter<AdapterRecentlyDeleted.ViewHolder>() {

    private var callBack: (RecentlyDeleted) -> Unit = {}
    private val selectedItems = mutableSetOf<RecentlyDeleted>() // Store multiple selected items
    private var isAllSelected = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentlyDeletedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        holder.bind(data, isAllSelected)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.stopUpdates()
    }

    fun setOnClickListener(callback: (RecentlyDeleted) -> Unit) {
        this.callBack = callback
    }

    /**
     * Selects or deselects all items.
     */
    fun updateSelectionState(selectAll: Boolean) {
        isAllSelected = selectAll
        selectedItems.clear()

        if (selectAll) {
            selectedItems.addAll(items)
        }
        notifyDataSetChanged()
    }

    /**
     * Returns the selected items.
     */
    fun getSelectedItems(): List<RecentlyDeleted> {
        return selectedItems.toList()
    }

    inner class ViewHolder(val binding: ItemRecentlyDeletedBinding) : RecyclerView.ViewHolder(binding.root) {
        private var updateHandler: Handler? = null

        fun bind(data: RecentlyDeleted, isAllSelected2: Boolean) {
            binding.imgLogo.setImageResource(R.drawable.ic_otp_avatar)
            binding.tvServiceName.text = data.name

            val isSelected = selectedItems.contains(data)

            // Update UI based on selection state
            if (isSelected) {
                binding.imgRadioButton.setImageResource(R.drawable.ic_check_box)
            } else {
                binding.imgRadioButton.setImageResource(R.drawable.ic_checkbox_un_selected)
            }

            binding.root.setOnClickListener {
                if (selectedItems.contains(data)) {
                    selectedItems.remove(data) // Deselect
                } else {
                    selectedItems.add(data) // Select
                }

                notifyItemChanged(bindingAdapterPosition)
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
                        binding.progressIndicator.progress = remainingSeconds
                        updateHandler?.postDelayed(this, 1000L)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        binding.tvTotp.text = itemView.context.getString(R.string.string_error)
                    }
                }
            }

            updateHandler?.post(updateTotp)
        }

        fun stopUpdates() {
            updateHandler?.removeCallbacksAndMessages(null)
        }
    }
}
