package com.husnain.authy.ui.fragment.main.addNewAccount

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.husnain.authy.R
import com.husnain.authy.data.room.EntityTotp
import com.husnain.authy.databinding.FragmentAddNewBinding
import com.husnain.authy.ui.fragment.main.home.VmHome
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import io.github.g00fy2.quickie.content.QRContent

@AndroidEntryPoint
class AddNewFragment : Fragment() {
    private var _binding: FragmentAddNewBinding? = null
    private val binding get() = _binding!!
    private val scanQrCodeLauncher = registerForActivityResult(ScanQRCode(), ::handleResult)
    private val vmHome:VmHome by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddNewBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
        setUpObservers()
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }

        binding.imgScanQr.setOnClickListener {
            scanQrCodeLauncher.launch(null)
        }

        binding.btnAddAccountManually.setOnClickListener {
            navigate(R.id.action_addNewFragment_to_addAccountManuallyFragment)
        }
    }

    private fun setUpObservers() {
        vmHome.insertState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    navigate(R.id.action_addNewFragment_to_homeFragment)
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        })
    }

    private fun handleResult(result: QRResult) {
        when (result) {
            is QRResult.QRSuccess -> {
                val content = result.content
                when (content) {
                    is QRContent.Plain -> {
                        val uri = content.rawValue
                        if (uri != null) {
                            if (uri.startsWith("otpauth://")) {
                                handleTOTPUri(uri)
                            } else {
                                showCustomToast("Scanned content: $uri")
                            }
                        }
                    }

                    else -> {
                        showCustomToast("Scanned qr is not a 2FA Qr")
                    }
                }
            }

            QRResult.QRUserCanceled -> {

            }

            QRResult.QRMissingPermission -> {
                showCustomToast("Missing permission to scan QR codes.")
            }

            is QRResult.QRError -> {
                val errorMessage =
                    "${result.exception.javaClass.simpleName}: ${result.exception.localizedMessage}"
                showCustomToast("Error occurred: $errorMessage")
            }
        }
    }

    private fun handleTOTPUri(uri: String) {
        try {
            val uriObject = Uri.parse(uri)
            val secretKey = uriObject.getQueryParameter("secret")

            // Extract the label from the path portion, removing leading '/' if it exists
            val path = uriObject.path?.removePrefix("/")
            val label =
                path?.substringAfter("totp/")?.substringBefore(":") ?: path?.substringAfter("totp/")

            if (secretKey != null && label != null) {
                Log.d("secret key data","Secret: $secretKey\nLabel: $label")
                showCustomToast("Secret: $secretKey\nLabel: $label")
                vmHome.insertSecretData(EntityTotp(0,label,secretKey))
            } else {
                showCustomToast("Something went wrong, please try again")
            }
        } catch (e: Exception) {
            showCustomToast("Error parsing TOTP URI: ${e.localizedMessage}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}