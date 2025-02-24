package com.husnain.authy.ui.fragment.main.importFromGoogle

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.husnain.authy.R
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.FragmentAddFromSameDeviceBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.fragment.main.addNewAccount.VmAddAccount
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.Flags
import com.husnain.authy.utls.OtpMigration
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class AddFromSameDeviceFragment : Fragment() {
    private var _binding: FragmentAddFromSameDeviceBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics
    private val vmAddAccount: VmAddAccount by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddFromSameDeviceBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
        setUpObservers()
    }


    private fun setUpObservers() {
        vmAddAccount.insertState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    Flags.isComingAfterAddingTotpData = true
                    navigate(R.id.homeFragment)
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        }
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.btnSelectFromGallery.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        openGalleryRequest.launch(Intent.createChooser(intent, "Scan Gallery"))
    }

    private val openGalleryRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                it.data?.data?.let { uri ->
                    decodeQRCode(uri)
                }
            }
        }

    private fun decodeQRCode(imageUri: Uri) {
        try {
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    val inputStream = context?.contentResolver?.openInputStream(imageUri)
                    BitmapFactory.decodeStream(inputStream)
                }

                val intArray = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                val source: LuminanceSource =
                    RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                val reader = QRCodeReader()
                val result = reader.decode(binaryBitmap)

                // The QR code content is in result.text
                val qrCodeContent = result.text

                handleQrContent(qrCodeContent)
            }
        } catch (e: Exception) {
            showCustomToast(resources.getString(R.string.string_something_went_wrong_please_try_again))
        }
    }

    private fun handleQrContent(qrContent: String?) {
        if (qrContent != null) {
            logQRCodeScanned()
            if (qrContent.startsWith("otpauth://") || qrContent.startsWith("otpauth-migration://")) {
                handleTOTPUri(qrContent)
            } else {
                showCustomToast(getString(R.string.string_something_went_wrong_please_try_again))
            }
        }
    }

    private fun handleTOTPUri(uri: String) {
        try {
            if (uri.startsWith("otpauth://")) {
                val uriObject = Uri.parse(uri)
                val secretKey = uriObject.getQueryParameter("secret")

                val path = uriObject.path?.removePrefix("/")
                val label = path?.substringAfter("totp/")?.substringBefore(":")
                    ?: path?.substringAfter("totp/")

                if (secretKey != null && label != null) {
                    Log.d("secret key data", "Secret: $secretKey\nLabel: $label")
                    vmAddAccount.insertSecretData(EntityTotp(0, label, secretKey))
                } else {
                    showCustomToast(getString(R.string.string_something_went_wrong_please_try_again))
                }
            } else {
                val dataUri = uri.replace("otpauth-migration://offline?data=", "")
                val decodedDataList = OtpMigration.getGoogleAuthData(dataUri)

                if (decodedDataList != null) {
                    for (data in decodedDataList) {
                        if (data.name.isEmpty() && data.issuer.isNotEmpty()) {
                            vmAddAccount.insertSecretData(
                                EntityTotp(
                                    0,
                                    data.issuer,
                                    data.secretBase32
                                )
                            )
                        } else {
                            vmAddAccount.insertSecretData(
                                EntityTotp(
                                    0,
                                    data.name,
                                    data.secretBase32
                                )
                            )
                        }
                        println("Account Name: ${data.name}, issuer: ${data.issuer}, Secret: ${data.secretBase32}")
                    }
                } else {
                    showCustomToast(getString(R.string.string_something_went_wrong_please_try_again))
                }
            }

        } catch (e: Exception) {
            showCustomToast("Error parsing TOTP URI: ${e.localizedMessage}")
        }
    }

    private fun logQRCodeScanned() {
        val bundle = Bundle().apply {
            putString("scan_result", "success")
        }
        firebaseAnalytics.logEvent("qr_code_scanned", bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}