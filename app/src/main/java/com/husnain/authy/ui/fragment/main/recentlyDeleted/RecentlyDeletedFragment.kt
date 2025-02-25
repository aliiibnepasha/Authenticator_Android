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
    private var selectedDataToDelete: List<RecentlyDeleted>? = null
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
                getString(R.string.restore),
                getString(R.string.reuse_account_for_2_factor_authentication),
                getString(R.string.restore),
                true,
                onPrimaryClick = {
                    selectedDataToDelete?.let { it1 ->
                        it1.forEach {data ->
                            vmRecentlyDeleted.restoreOrDelete(
                                data,
                                OperationType.RESTORE
                            )
                        }
                    }
                })
        }

        binding.btnDeletePermanently.setOnClickListener {
            showBottomSheetDialog(
                getString(R.string.permanently_delete),
                getString(R.string.once_removed_the_current_device_will_not_be_used_for_this_account_s_2_factor_authentication),
                getString(R.string.delete),
                false,
                onPrimaryClick = {
                    selectedDataToDelete?.let { it1 ->
                        it1.forEach {data ->
                            vmRecentlyDeleted.restoreOrDelete(
                                data,
                                OperationType.PERMANENTLY_DELETE
                            )
                        }
                    }
                })
        }

        binding.btnRestoreAll.setOnClickListener {
            showBottomSheetDialog(
                getString(R.string.restore),
                getString(R.string.reuse_account_for_2_factor_authentication),
                getString(R.string.restore),
                true,
                onPrimaryClick = {
                    vmRecentlyDeleted.restoreOrDelete(null, OperationType.RESTORE_ALL)
                })
        }

        binding.btnDeleteAll.setOnClickListener {
            showBottomSheetDialog(
                getString(R.string.permanently_delete),
                getString(R.string.once_removed_the_current_device_will_not_be_used_for_this_account_s_2_factor_authentication),
                getString(R.string.delete),
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
                binding.imgCheckBoxAll.setImageResource(R.drawable.ic_check_box)
            }else{
                adapter.updateSelectionState(false)
                binding.lyForOneSelection.gone()
                binding.lyForAll.gone()
                binding.imgCheckBoxAll.setImageResource(R.drawable.ic_checkbox_un_selected)
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
            binding.rvRecentlyDeleted.adapter = adapter

            adapter.setOnClickListener { recentData ->
                selectedDataToDelete = adapter.getSelectedItems()
                binding.lyForAll.gone()
                binding.lyForOneSelection.visible()
                binding.imgCheckBoxAll.setImageResource(R.drawable.ic_checkbox_un_selected)
            }
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