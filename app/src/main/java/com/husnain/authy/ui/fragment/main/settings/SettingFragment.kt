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
import com.husnain.authy.ui.activities.AuthActivity
import com.husnain.authy.ui.fragment.auth.VmAuth
import com.husnain.authy.ui.fragment.main.home.VmHome
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.decodeQRCode
import com.husnain.authy.utls.handleTOTPURI
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.openGallery
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.setupGalleryPicker
import com.husnain.authy.utls.setupQrCodeScanner
import com.husnain.authy.utls.showBottomSheetDialog
import com.husnain.authy.utls.startActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val vmAuth: VmAuth by viewModels()
    private val vmHome: VmHome by viewModels()
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
        setOnClickListener()
        setUpObserver()
        handleDataFromGallery()
        handleDataFromCamera()
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.lyGetPremium.setOnClickListener {

        }
        binding.lyLocalizeLanguages.setOnClickListener {
            navigate(R.id.action_settingFragment_to_localizeFragment)
        }
        binding.lyDeleteAccount.setOnClickListener {
            showDeleteAccountDialog {
                vmAuth.deleteAccount()
            }
        }
        binding.lyBackupAndSync.setOnClickListener {
            navigate(R.id.action_settingFragment_to_backUpFragment)
        }
        binding.lyRecentlyDeleted.setOnClickListener {
            navigate(R.id.action_settingFragment_to_recentlyDeletedFragment)
        }
        binding.btnLogout.setOnClickListener {
            showBottomSheetDialog("Logout",onPrimaryClick = {
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
                    startActivity(AuthActivity::class.java)
                    requireActivity().finish()
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
                    startActivity(AuthActivity::class.java)
                    requireActivity().finish()
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
        scanQrCodeLauncher = setupQrCodeScanner(
            onSuccess = { qrContent ->
                processTOTPURI(qrContent)
            },
            onNot2FAQR = {
                showCustomToast("Scanned QR is not a 2FA QR")
            },
            onMissingPermission = {
                showCustomToast("Missing permission to scan QR codes.")
            },
            onError = { errorMessage ->
                showCustomToast("Error occurred: $errorMessage")
            }
        )
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


    //dialog
    private fun showDeleteAccountDialog(onConfirm: () -> Unit) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Delete Account")
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.")
        builder.setPositiveButton("Yes") { _, _ -> onConfirm() }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}