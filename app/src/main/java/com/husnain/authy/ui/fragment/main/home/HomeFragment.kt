package com.husnain.authy.ui.fragment.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.FragmentHomeBinding
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val vmHome: VmHome by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
        setUpObservers()
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
            binding.btnAddAccountFirstTime.gone()
            binding.rvHomeTotp.visible()
            binding.btnAddNewAccountWhenSomeAccountAdded.visible()

            val dataList = data.map {
                ModelTotp(it.secretKey, it.serviceName)
            }

            val adapter = AdapterHomeTotp(dataList) {

            }
            binding.rvHomeTotp.adapter = adapter
        } else {
            binding.lyLinearAddAccountFirstTime.visible()
            binding.rvHomeTotp.gone()
            binding.btnAddNewAccountWhenSomeAccountAdded.gone()
        }
    }


    private fun setOnClickListener() {
        binding.btnAddNewAccountWhenSomeAccountAdded.setOnClickListener {
            navigate(R.id.action_homeFragment_to_addNewFragment)
        }

        binding.btnAddAccountFirstTime.setOnClickListener {
            navigate(R.id.action_homeFragment_to_addNewFragment)
        }

        binding.imgSearch.setOnClickListener {
            navigate(R.id.action_homeFragment_to_searchFragment)
        }

        binding.imgPremium.setOnClickListener {
            navigate(R.id.action_homeFragment_to_subscriptionFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}