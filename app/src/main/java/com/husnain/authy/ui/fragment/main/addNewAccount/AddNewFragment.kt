package com.husnain.authy.ui.fragment.main.addNewAccount

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.husnain.authy.R
import com.husnain.authy.databinding.FragmentAddNewBinding
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import io.github.g00fy2.quickie.content.QRContent

class AddNewFragment : Fragment() {
    private var _binding: FragmentAddNewBinding? = null
    private val binding get() = _binding!!
    val scanQrCodeLauncher = registerForActivityResult(ScanQRCode(), ::handleResult)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddNewBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
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
            val secret = uriObject.getQueryParameter("secret")

            // Extract the label from the path portion, removing leading '/' if it exists
            val path = uriObject.path?.removePrefix("/")
            val label =
                path?.substringAfter("totp/")?.substringBefore(":") ?: path?.substringAfter("totp/")

            if (secret != null && label != null) {
                showCustomToast("Secret: $secret\nLabel: $label")
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