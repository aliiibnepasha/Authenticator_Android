package com.husnain.authy.utls

import android.widget.Filter
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.Method
import java.util.Locale

class SearchFilter<T>(
    private val originalList: List<T>,
    private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>
) : Filter() {

    private var filteredList: MutableList<T> = ArrayList()

    init {
        filteredList.addAll(originalList)
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        filteredList.clear()
        val filterResults = FilterResults()

        if (constraint.isNullOrBlank()) {
            filteredList.addAll(originalList)
        } else {
            val filterPattern = constraint.toString().lowercase(Locale.ROOT).trim()

            for (item in originalList) {
                if (item.toString().lowercase(Locale.ROOT).contains(filterPattern)) {
                    filteredList.add(item)
                }
            }
        }

        filterResults.values = filteredList
        filterResults.count = filteredList.size
        return filterResults
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        val filteredList = results?.values as? List<*> ?: return
        val setItemsMethod = findSetItemsMethod(adapter)
        setItemsMethod?.invoke(adapter, filteredList)
    }

    private fun findSetItemsMethod(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>): Method? {
        return try {
            adapter.javaClass.getDeclaredMethod("setItems", List::class.java)
        } catch (e: NoSuchMethodException) {
            null
        }
    }
}
