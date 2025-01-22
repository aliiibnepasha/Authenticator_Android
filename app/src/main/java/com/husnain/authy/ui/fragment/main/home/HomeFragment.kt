package com.husnain.authy.ui.fragment.main.home

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.data.models.ModelTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.data.room.tables.RecentlyDeleted
import com.husnain.authy.databinding.FragmentHomeBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomDialogs
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.PermissionUtils
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.showBottomSheetDialog
import com.husnain.authy.utls.startActivity
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdapterHomeTotp
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var auth: FirebaseAuth
    private val vmHome: VmHome by viewModels()
    var isDeleted = false

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
        askPermissions()
        if (!vmHome.isNavigationTriggered) {
            autoScreensStartup()
            vmHome.isNavigationTriggered = true
        }
        setOnClickListener()
        setUpObservers()
    }


    private fun setOnClickListener() {
        binding.btnAddNewAccountWhenSomeAccountAdded.setOnClickListener {
            navigate(R.id.action_homeFragment_to_addAccountFragment)
        }

        binding.btnAddAccountFirstTime.setOnClickListener {
            navigate(R.id.action_homeFragment_to_addAccountFragment)
        }

        binding.imgSearch.setOnClickListener {
            navigate(R.id.action_homeFragment_to_searchFragment)
        }

        binding.imgPremium.setOnClickListener {
            navigate(R.id.action_homeFragment_to_subscriptionFragment)
        }
    }


    private fun askPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            return
        }
        val isPermissionGranted = PermissionUtils.handlePermissions(requireActivity(), permissions, 1)
        if (isPermissionGranted) return
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
                    showCustomToast("Error: ${state.message}")
                }
            }
        })

        vmHome.deleteState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    if (isDeleted){
                        showCustomToast(getString(R.string.string_deleted_successfully))
                        isDeleted = false
                    }
                    vmHome.fetchAllTotp()
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        })

        vmHome.insertRecentlyDeletedState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }
                is DataState.Success -> {

                }
                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        })
    }

    private fun setupAdapter(data: List<EntityTotp>) {
        if (data.isNotEmpty()) {
            binding.btnAddAccountFirstTime.gone()
            binding.rvHomeTotp.visible()
            binding.btnAddNewAccountWhenSomeAccountAdded.visible()
            binding.lyLinearAddAccountFirstTime.gone()

            val dataList = data.map {
                ModelTotp(it.secretKey, it.serviceName)
            }
            adapter = AdapterHomeTotp(dataList)

            //Long click to show the delete bottom sheet
            adapter.setOnLongClickListener { totpData ->
                showBottomSheetDialog(getString(R.string.string_delete), onPrimaryClick = {
                    vmHome.insertToRecentlyDeleted(RecentlyDeleted(totpData.serviceName,totpData.secretKey))
                    vmHome.deleteTotp(totpData.secretKey)
                    isDeleted = true
                })
            }
            //end click

            binding.rvHomeTotp.adapter = adapter
        } else {
            binding.rvHomeTotp.gone()
            binding.btnAddNewAccountWhenSomeAccountAdded.gone()
            binding.btnAddAccountFirstTime.visible()
            binding.lyLinearAddAccountFirstTime.visible()
        }
    }

    private fun autoScreensStartup(){
        when{
            isToShowSubscriptionScreen() ->{
                navigate(R.id.action_homeFragment_to_subscriptionFragment)
            }
            isToShowSigninDialogForSync() ->{
                CustomDialogs.dialogAuthForAutoSync(requireContext(),layoutInflater,
                    login = {
                        Constants.isComingToAuthFromGuest = true
                        Constants.isComingToAuthFromGuestToSignIn = true
                        startActivity(AuthActivity::class.java)
                    },
                    signup = {
                        Constants.isComingToAuthFromGuest = true
                        startActivity(AuthActivity::class.java)
                    }
                    )
            }
            else -> return
        }
    }
    private fun isToShowSubscriptionScreen() : Boolean{
        return when{
            !preferenceManager.isSubscriptionActive() && preferenceManager.isToShowSubsScreenAsDialog()  -> true
            else -> false
        }
    }

    private fun isToShowSigninDialogForSync(): Boolean{
        val uid = auth.currentUser?.uid
        val isUserLoggedIn = uid != null
        return when{
            preferenceManager.isSubscriptionActive() && !isUserLoggedIn -> true
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}