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
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentSettingBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.ui.fragment.auth.VmAuth
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.decodeQRCode
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.handleTOTPURI
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.openGallery
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.setupGalleryPicker
import com.husnain.authy.utls.showBottomSheetDialog
import com.husnain.authy.utls.showDeleteAccountConfirmationBottomSheet
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val vmAuth: VmAuth by viewModels()
    private val vmSettings: VmSettings by viewModels()
    @Inject lateinit var preferenceManager: PreferenceManager
    private lateinit var scanQrCodeLauncher: ActivityResultLauncher<Nothing?>
    private lateinit var galleryPickerLauncher: ActivityResultLauncher<Intent>
    val KEY_HEADER_TITLE = "headerTitle"
    val KEY_LINK_TO_LOAD = "linkToLoad"
    private var scrollPosition: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        scrollPosition = savedInstanceState?.getInt("scroll_position") ?: 0
        binding.scrollView.post {
            binding.scrollView.scrollTo(0, preferenceManager.getSettingScrollPosition())
        }
        inIt()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        inItUi()
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
            binding.apply {
                btnLogout.gone()
                tvDontHaveAndAccount.visible()
                tvSignup.visible()
                btnLogin.visible()
            }
        }else{
            binding.apply {
                btnLogout.visible()
                tvDontHaveAndAccount.gone()
                tvSignup.gone()
                btnLogin.gone()
            }
        }

    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.tvSignup.setOnClickListener {
            if (preferenceManager.isGuestUser()) {
                Constants.isComingToAuthFromGuest = true
                Constants.isComingToAuthFromGuestSignUp = true
                val intent = Intent(requireContext(), AuthActivity::class.java)
                startActivity(intent)
            }
        }
        binding.btnLogin.setOnClickListener {
            if (preferenceManager.isGuestUser()) {
                Constants.isComingToAuthFromGuest = true
                val intent = Intent(requireContext(), AuthActivity::class.java)
                startActivity(intent)
            }
        }

        binding.lyGetPremium.setOnClickListener {
//            navigate(R.id.action_settingFragment_to_otpFragment)
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

        //Info and share
        binding.lyTermsAndConsidtions.setOnClickListener {
            val url = "https://sites.google.com/view/authenticatorapp-termofuse/home"
            val bundle = Bundle().apply {
                putString(KEY_HEADER_TITLE,resources.getString(R.string.string_terms_amp_condition))
                putString(KEY_LINK_TO_LOAD,url)
            }
            navigate(R.id.action_settingFragment_to_webViewFragment,bundle)
        }
        binding.lyPrivacyPolicy.setOnClickListener {
            val url = "https://sites.google.com/view/authenticatorapp-privacypolicy/home."
            val bundle = Bundle().apply {
                putString(KEY_HEADER_TITLE,resources.getString(R.string.string_privacy_policy))
                putString(KEY_LINK_TO_LOAD,url)
            }
            navigate(R.id.action_settingFragment_to_webViewFragment,bundle)
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

        vmSettings.insertState.observe(viewLifecycleOwner, Observer { state ->
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
                vmSettings.insertSecretData(entity)
            },
            onError = { error ->
                showCustomToast(error)
            },
        )
    }

    override fun onPause() {
        super.onPause()
        binding.scrollView.let {
            preferenceManager.saveSettingScrollPosition(it.scrollY)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}