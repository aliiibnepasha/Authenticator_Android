package com.husnain.authy.ui.fragment.main.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.husnain.authy.R
import com.husnain.authy.databinding.BottomSheetDeleteTotpBinding
import com.husnain.authy.databinding.BottomSheetLayoutBinding
import com.husnain.authy.databinding.FragmentSettingBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.ui.fragment.auth.VmAuth
import com.husnain.authy.ui.fragment.main.home.VmHome
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.decodeQRCode
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.handleTOTPURI
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.openGallery
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.setupGalleryPicker
//import com.husnain.authy.utls.setupQrCodeScanner
import com.husnain.authy.utls.showBottomSheetDialog
import com.husnain.authy.utls.showDeleteAccountConfirmationBottomSheet
import com.husnain.authy.utls.startActivity
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val vmAuth: VmAuth by viewModels()
    private val vmHome: VmHome by viewModels()
    @Inject lateinit var preferenceManager: PreferenceManager
    private lateinit var scanQrCodeLauncher: ActivityResultLauncher<Nothing?>
    private lateinit var galleryPickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        inItUi()
        setOnClickListener()
        setUpObserver()
        handleDataFromGallery()
        handleDataFromCamera()
    }

    private fun inItUi() {
        if (preferenceManager.isGuestUser()){
            binding.btnLogout.gone()
        }else{
            binding.btnLogout.visible()
        }
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.lyGetPremium.setOnClickListener {

        }
        binding.lyGetPremium.setOnClickListener {
            navigate(R.id.action_settingFragment_to_subscriptionFragment)
        }
        binding.lyLocalizeLanguages.setOnClickListener {
            navigate(R.id.action_settingFragment_to_localizeFragment)
        }
        binding.lyDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationBottomSheet {
                vmAuth.deleteAccount()
            }
        }
        binding.lyBackupAndSync.setOnClickListener {
            if (!preferenceManager.isGuestUser()){
                if (!preferenceManager.isSubscriptionActive()){
                    navigate(R.id.action_settingFragment_to_subscriptionFragment)
                }else{
                    navigate(R.id.action_settingFragment_to_backUpFragment)
                }
            }else{
                navigate(R.id.action_settingFragment_to_subscriptionFragment)
            }
        }
        binding.lyRecentlyDeleted.setOnClickListener {
            navigate(R.id.action_settingFragment_to_recentlyDeletedFragment)
        }
        binding.btnLogout.setOnClickListener {
            showBottomSheetDialog(resources.getString(R.string.string_logout),onPrimaryClick = {
                vmAuth.logout()
            })
        }
        binding.lyImportGoogleAuthData.setOnClickListener {
            showBottomSheetDialog(
                onGalleryClick = {
                    openGallery(galleryPickerLauncher)
                },
                onCameraClick = {
                    scanQrCodeLauncher.launch(null)
                }
            )
        }
    }

    private fun setUpObserver() {
        vmAuth.logoutState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    popBack()
                }

                is DataState.Error -> {
                    showCustomToast(state.message)
                }
            }
        })

        vmAuth.deleteAccountState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    popBack()
                }

                is DataState.Error -> {
                    showCustomToast(state.message)
                }
            }
        })

        vmHome.insertState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    navigate(R.id.action_settingFragment_to_homeFragment)
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        })
    }

    private fun handleDataFromCamera() {
//        scanQrCodeLauncher = setupQrCodeScanner(
//            onSuccess = { qrContent ->
//                processTOTPURI(qrContent)
//            },
//            onNot2FAQR = {
//                showCustomToast("Scanned QR is not a 2FA QR")
//            },
//            onMissingPermission = {
//                showCustomToast("Missing permission to scan QR codes.")
//            },
//            onError = { errorMessage ->
//                showCustomToast("Error occurred: $errorMessage")
//            }
//        )
    }

    private fun handleDataFromGallery() {
        galleryPickerLauncher = setupGalleryPicker { uri ->
            requireContext().decodeQRCode(uri,
                onSuccess = { qrCodeContent ->
                    processTOTPURI(qrCodeContent)
                },
                onError = { error ->
                    showCustomToast("Error decoding QR code: ${error.localizedMessage}")
                }
            )
        }
    }

    private fun processTOTPURI(qrContent: String) {
        handleTOTPURI(
            uri = qrContent,
            onInsertSecret = { entity ->
                vmHome.insertSecretData(entity)
            },
            onError = { error ->
                showCustomToast(error)
            },
        )
    }


    override fun onPause() {
        super.onPause()
        vmAuth.deleteAccountState.removeObservers(viewLifecycleOwner)
        vmAuth.logoutState.removeObservers(viewLifecycleOwner)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}