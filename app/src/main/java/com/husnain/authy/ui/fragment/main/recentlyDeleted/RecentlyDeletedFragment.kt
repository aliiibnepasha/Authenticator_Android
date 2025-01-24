package com.husnain.authy.ui.fragment.main.recentlyDeleted

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.databinding.FragmentRecentlyDeletedBinding
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.OperationType
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecentlyDeletedFragment : Fragment() {
    private var _binding: FragmentRecentlyDeletedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdapterRecentlyDeleted
    private var selectedDataToDelete: RecentlyDeleted? = null
    private val vmRecentlyDeleted: VmRecentlyDeleted by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentlyDeletedBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        fetchData()
        setOnClickListener()
        setUpObservers()
    }

    private fun fetchData() {
        vmRecentlyDeleted.fetchRecentlyDeleted()
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }

        binding.btnRestore.setOnClickListener {
            selectedDataToDelete?.let { it1 ->
                vmRecentlyDeleted.restoreOrDelete(
                    it1,
                    OperationType.RESTORE
                )
            }
        }

        binding.btnDeletePermanently.setOnClickListener {
            selectedDataToDelete?.let { it1 ->
                vmRecentlyDeleted.restoreOrDelete(
                    it1,
                    OperationType.PERMANENTLY_DELETE
                )
            }
        }

        binding.btnRestoreAll.setOnClickListener {
            vmRecentlyDeleted.restoreOrDelete(null, OperationType.RESTORE_ALL)
        }

        binding.btnDeleteAll.setOnClickListener {
            vmRecentlyDeleted.restoreOrDelete(null, OperationType.DELETE_ALL)
        }
    }

    private fun setUpObservers() {
        vmRecentlyDeleted.fetchState.observe(viewLifecycleOwner, Observer { state ->
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
                    showCustomToast("Error: ${state.message}")
                }
            }
        })

        vmRecentlyDeleted.restoreState.observe(viewLifecycleOwner, Observer { state ->
            binding.loadingView.stop()
            when (state) {
                is DataState.Loading -> {
                    binding.loadingView.start()
                }

                is DataState.Success -> {
                    //Fetch here again to refresh the list
                    vmRecentlyDeleted.fetchRecentlyDeleted()
                    Constants.isComingAfterRestore = true
//                    //Fetch for home so that when user go back to home user can see the data
//                    //Note(you can also do it in home fragment onRestart
//                    vmRecentlyDeleted.fetchAllTotp()
                    showCustomToast("Action completed successfully")
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        })
    }

    private fun setupAdapter(data: List<RecentlyDeleted>) {
        if (data.isNotEmpty()) {
            binding.tvNoDataFound.gone()
            binding.lyForOneSelection.gone()
            binding.lyForAll.visible()

            adapter = AdapterRecentlyDeleted(data)
            adapter.setOnClickListener { recentData ->
                binding.lyForAll.gone()
                binding.lyForOneSelection.visible()
                selectedDataToDelete = recentData
            }
            binding.rvRecentlyDeleted.adapter = adapter
        } else {
            binding.tvNoDataFound.visible()
            binding.lyForOneSelection.gone()
            binding.lyForAll.gone()
            binding.rvRecentlyDeleted.gone()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}