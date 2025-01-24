package com.husnain.authy.ui.fragment.main.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentSettingBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.decodeQRCode
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.handleTOTPURI
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.openGallery
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.progress.showBottomSheetDialog
import com.husnain.authy.utls.progress.showDeleteAccountConfirmationBottomSheet
import com.husnain.authy.utls.setupGalleryPicker
import com.husnain.authy.utls.showBottomSheetDialog
import com.husnain.authy.utls.startActivity
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val vmSettings: VmSettings by viewModels()

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var auth: FirebaseAuth
    private lateinit var scanQrCodeLauncher: ActivityResultLauncher<Nothing?>
    private lateinit var galleryPickerLauncher: ActivityResultLauncher<Intent>
    private val KEY_HEADER_TITLE = "headerTitle"
    private val KEY_LINK_TO_LOAD = "linkToLoad"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        inIt()
        updateUiBasedOnUserState()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateUiBasedOnUserState()
    }

    private fun inIt() {
        setOnClickListener()
        setUpObserver()
        handleDataFromGallery()
        handleDataFromCamera()
    }

    private fun updateUiBasedOnUserState() {
        val userId = auth.currentUser?.uid
        val isUserLoggedIn = userId != null
        val isUserHavePremium = preferenceManager.isSubscriptionActive()

        binding.apply {
            // Handle premium state
            if (isUserHavePremium) {
                lyGetPremium.gone()
                imgSyncPremium.gone()
            } else {
                lyGetPremium.visible()
                imgSyncPremium.visible()
            }

            // Handle login state
            if (!isUserLoggedIn) {
                btnLogout.gone()
                tvDontHaveAndAccount.visible()
                tvSignup.visible()
                btnLogin.visible()
            } else {
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

        //Authentications
        binding.tvSignup.setOnClickListener {
            if (preferenceManager.isGuestUser()) {
                Constants.isComingToAuthFromGuest = true
                startActivity(AuthActivity::class.java)
            }
        }
        binding.btnLogin.setOnClickListener {
            if (preferenceManager.isGuestUser()) {
                Constants.isComingToAuthFromGuest = true
                //below flag is used to check at signup screen is to take user to login screen directly 
                Constants.isComingToAuthFromGuestToSignIn = true
                startActivity(AuthActivity::class.java)
            }
        }

        //Navto buy premium
        binding.lyGetPremium.setOnClickListener {
            navigate(R.id.action_settingFragment_to_subscriptionFragment)
        }

        //nav to change lang 
        binding.lyLocalizeLanguages.setOnClickListener {
            navigate(R.id.action_settingFragment_to_localizeFragment)
        }

        //delete account
        binding.lyDeleteAccount.setOnClickListener {
            //show confirmation bottom sheet to user on delete tv click delete user data and account from db
            showDeleteAccountConfirmationBottomSheet {
                vmSettings.deleteAccount()
            }
        }

        //Backup and sync
        binding.lyBackupAndSync.setOnClickListener {
            val isUserLoggedIn = auth.currentUser?.uid != null
            val isUserHavePremium = preferenceManager.isSubscriptionActive()
            Log.d(Constants.TAG, "premium = $isUserHavePremium, loggedIn = $isUserLoggedIn")

            when {
                // If the user is not subscribed, take them to the subscription screen
                !isUserHavePremium -> {
                    navigate(R.id.action_settingFragment_to_subscriptionFragment)
                }

                // If the user is subscribed but not logged in, take them to the auth activity
                isUserHavePremium && !isUserLoggedIn -> {
                    Constants.isComingToAuthFromGuest = true
                    startActivity(AuthActivity::class.java)
                }

                // If the user is subscribed and logged in, take them to the backup screen
                isUserHavePremium && isUserLoggedIn -> {
                    navigate(R.id.action_settingFragment_to_backUpFragment)
                }
            }
        }


        //Recently deleted
        binding.lyRecentlyDeleted.setOnClickListener {
            navigate(R.id.action_settingFragment_to_recentlyDeletedFragment)
        }

        //logout
        binding.btnLogout.setOnClickListener {
            showBottomSheetDialog(resources.getString(R.string.string_logout), onPrimaryClick = {
                vmSettings.logout()
            })
        }

        //google authenticator import
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
            navToTermsAndConditions()
        }
        binding.lyPrivacyPolicy.setOnClickListener {
            navToPrivacyPolicy()
        }
    }

    private fun setUpObserver() {
        vmSettings.logoutState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    popBack()
                }

                is DataState.Error -> {
                    Log.e(Constants.TAG, "Error: ${state.message}")
                    showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
                }
            }
        })

        vmSettings.deleteAccountState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    popBack()
                }

                is DataState.Error -> {
                    Log.e(Constants.TAG, "Error: ${state.message}")
                    showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
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
                    Log.e(Constants.TAG, "Error: ${state.message}")
                    showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
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

    //info navigations
    private fun navToTermsAndConditions() {
        val url = "https://sites.google.com/view/authenticatorapp-termofuse/home"
        val bundle = Bundle().apply {
            putString(KEY_HEADER_TITLE, resources.getString(R.string.string_terms_amp_condition))
            putString(KEY_LINK_TO_LOAD, url)
        }
        navigate(R.id.action_settingFragment_to_webViewFragment, bundle)
    }

    private fun navToPrivacyPolicy() {
        val url = "https://sites.google.com/view/authenticatorapp-privacypolicy/home"
        val bundle = Bundle().apply {
            putString(KEY_HEADER_TITLE, resources.getString(R.string.string_privacy_policy))
            putString(KEY_LINK_TO_LOAD, url)
        }
        navigate(R.id.action_settingFragment_to_webViewFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}