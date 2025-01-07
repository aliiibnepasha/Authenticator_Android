package com.husnain.authy.ui.fragment.main.localization

import android.graphics.Color
import android.graphics.ColorSpace.Model
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.husnain.authy.data.ModelLanguage
import com.husnain.authy.databinding.ItemLanguageTextBinding

class AdapterLanguages(private val items: List<ModelLanguage>,private val callBack:(ModelLanguage) -> Unit) :
    RecyclerView.Adapter<AdapterLanguages.ViewHolder>() {

    private var selectedLang: String? = null

    fun updateSelectedLang(langShortType: String?) {
        selectedLang = langShortType
        notifyDataSetChanged() // Refresh all items to reflect changes
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):ViewHolder {
        val binding = ItemLanguageTextBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        holder.bind(data)
    }
    inner class ViewHolder(val binding: ItemLanguageTextBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: ModelLanguage){
            binding.tvLanguage.text = data.languageName

            if (data.langShortType == selectedLang) {
                binding.tvLanguage.setTextColor(Color.BLUE) // Highlighted color
            } else {
                binding.tvLanguage.setTextColor(Color.BLACK) // Default color
            }

            binding.lyLanguage.setOnClickListener {
                callBack.invoke(data)
            }
        }
    }

}