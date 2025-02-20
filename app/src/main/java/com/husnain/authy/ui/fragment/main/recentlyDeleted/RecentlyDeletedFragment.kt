package com.husnain.authy.ui.fragment.main.recentlyDeleted

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.husnain.authy.R
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.databinding.FragmentRecentlyDeletedBinding
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.OperationType
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.progress.showBottomSheetDialog
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecentlyDeletedFragment : Fragment() {
    private var _binding: FragmentRecentlyDeletedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdapterRecentlyDeleted
    private var selectedDataToDelete: RecentlyDeleted? = null
    private val vmRecentlyDeleted: VmRecentlyDeleted by viewModels()
    private var allSelected = false


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
            showBottomSheetDialog(
                "Restore",
                "Reuse account for 2-factor authentication",
                "Restore",
                true,
                onPrimaryClick = {
                    selectedDataToDelete?.let { it1 ->
                        vmRecentlyDeleted.restoreOrDelete(
                            it1,
                            OperationType.RESTORE
                        )
                    }
                })
        }

        binding.btnDeletePermanently.setOnClickListener {
            showBottomSheetDialog(
                "Permanently Delete",
                "Once removed, the current device will not be used for this account’s 2-factor authentication",
                "Delete",
                false,
                onPrimaryClick = {
                    selectedDataToDelete?.let { it1 ->
                        vmRecentlyDeleted.restoreOrDelete(
                            it1,
                            OperationType.PERMANENTLY_DELETE
                        )
                    }
                })
        }

        binding.btnRestoreAll.setOnClickListener {
            showBottomSheetDialog(
                "Restore",
                "Reuse account for 2-factor authentication",
                "Restore",
                true,
                onPrimaryClick = {
                    vmRecentlyDeleted.restoreOrDelete(null, OperationType.RESTORE_ALL)
                })
        }

        binding.btnDeleteAll.setOnClickListener {
            showBottomSheetDialog(
                "Permanently Delete",
                "Once removed, the current device will not be used for this account’s 2-factor authentication",
                "Delete",
                false,
                onPrimaryClick = {
                    vmRecentlyDeleted.restoreOrDelete(null, OperationType.DELETE_ALL)
                })
        }

        binding.imgCheckBoxAll.setOnClickListener {
            allSelected = !allSelected
            if (allSelected){
                adapter.updateSelectionState(true)
                binding.lyForAll.visible()
                binding.lyForOneSelection.gone()
                binding.imgCheckBoxAll.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                    PorterDuff.Mode.SRC_IN
                )
            }else{
                adapter.updateSelectionState(false)
                binding.lyForOneSelection.gone()
                binding.lyForAll.gone()
                binding.imgCheckBoxAll.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.black),
                    PorterDuff.Mode.SRC_IN
                )
            }
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
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        })
    }

    private fun setupAdapter(data: List<RecentlyDeleted>) {
        if (data.isNotEmpty()) {
            binding.lyForOneSelection.gone()

            adapter = AdapterRecentlyDeleted(data)
            adapter.setOnClickListener { recentData ->
                binding.lyForAll.gone()
                binding.lyForOneSelection.visible()
                selectedDataToDelete = recentData
                binding.imgCheckBoxAll.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.black),
                    PorterDuff.Mode.SRC_IN
                )
            }
            binding.rvRecentlyDeleted.adapter = adapter
        } else {
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