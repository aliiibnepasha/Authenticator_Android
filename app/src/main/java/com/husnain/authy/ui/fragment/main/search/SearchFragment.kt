package com.husnain.authy.ui.fragment.main.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.husnain.authy.data.models.ModelTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.FragmentSearchBinding
import com.husnain.authy.ui.fragment.main.home.AdapterHomeTotp
import com.husnain.authy.ui.fragment.main.home.VmHome
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.setupKeyboardDismissListener
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdapterHomeTotp
    private val vmHome:VmHome by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeyboardDismissListener(view)
        setupKeyboardDismissListener(binding.lottieAnimationViewNoData)
    }

    private fun inIt() {
        setOnClickListener()
        setUpObservers()
        setUpSearch()
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
    }

    private fun setUpSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (::adapter.isInitialized && adapter.itemCount >= 0) {
                    adapter.getFilter().filter(newText)
                }
                return true
            }
        })
    }

    private fun setUpObservers() {
        vmHome.totpListState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    val data = state.data
                    if (data != null) {
                        setupAdapter(data)
                    }
                }

                is DataState.Error -> {
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })

    }

    private fun setupAdapter(data: List<EntityTotp>) {
        if (data.isNotEmpty()) {
            val dataList = data.map {
                ModelTotp(it.secretKey, it.serviceName)
            }

            adapter = AdapterHomeTotp(dataList)
            binding.rvTotp.adapter = adapter

            adapter.emptyStateListener {
                if (it){
                    binding.lottieAnimationViewNoData.visible()
                    binding.lottieAnimationViewNoData.playAnimation()
                }else{
                    binding.lottieAnimationViewNoData.gone()
                    binding.lottieAnimationViewNoData.cancelAnimation()
                }
            }
        }else{
            binding.lottieAnimationViewNoData.visible()
            binding.lottieAnimationViewNoData.playAnimation()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}