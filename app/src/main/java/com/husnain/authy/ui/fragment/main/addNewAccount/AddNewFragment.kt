package com.husnain.authy.ui.fragment.main.addNewAccount

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
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.husnain.authy.R
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.BottomSheetLayoutBinding
import com.husnain.authy.databinding.FragmentAddNewBinding
import com.husnain.authy.ui.fragment.main.home.VmHome
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.OtpMigration
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import dagger.hilt.android.AndroidEntryPoint
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
            showBottomSheetDialog(
                onCameraClick = {
                    scanQrCodeLauncher.launch(null)
                },
                onGalleryClick = {
                    openGallery()
                }
            )
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

    //Region gallery
    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        openGalleryRequest.launch(Intent.createChooser(intent, "Scan Gallery"))
    }

    private val openGalleryRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                it.data?.data?.let { uri -> decodeQRCode(uri) }
            }
        }

    private fun decodeQRCode(imageUri: Uri) {
        try {
            val inputStream = context?.contentResolver?.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = QRCodeReader()
            val result = reader.decode(binaryBitmap)

            // The QR code content is in result.text
            val qrCodeContent = result.text

            handleQrContent(qrCodeContent)
        } catch (e: Exception) {

        }
    }

    //Region camera qr scanner and result
    private fun handleQrContent(qrContent: String?){
        if (qrContent != null) {
            if (qrContent.startsWith("otpauth://") || qrContent.startsWith("otpauth-migration://")) {
                handleTOTPUri(qrContent)
            } else {
                showCustomToast("Something went wrong")
            }
        }
    }

    private fun handleResult(result: QRResult) {
        when (result) {
            is QRResult.QRSuccess -> {
                val content = result.content
                when (content) {
                    is QRContent.Plain -> {
                        val uri = content.rawValue
                        handleQrContent(uri)
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
                val errorMessage = "${result.exception.javaClass.simpleName}: ${result.exception.localizedMessage}"
                showCustomToast("Error occurred: $errorMessage")
            }
        }
    }

    private fun handleTOTPUri(uri: String) {
        try {
            if (uri.startsWith("otpauth://")) {
                val uriObject = Uri.parse(uri)
                val secretKey = uriObject.getQueryParameter("secret")

                val path = uriObject.path?.removePrefix("/")
                val label = path?.substringAfter("totp/")?.substringBefore(":") ?: path?.substringAfter("totp/")

                if (secretKey != null && label != null) {
                    Log.d("secret key data","Secret: $secretKey\nLabel: $label")
                    vmHome.insertSecretData(EntityTotp(0,label,secretKey))
                } else {
                    showCustomToast("Something went wrong, please try again")
                }
            }else{
                val data = uri.replace("otpauth-migration://offline?data=","")
                val decodedDataList = OtpMigration.getGoogleAuthData(data)

                if (decodedDataList != null) {
                    for (data in decodedDataList){
                        if (data.name.isEmpty() && data.issuer.isNotEmpty()){
                            vmHome.insertSecretData(EntityTotp(0,data.issuer,data.secretBase32))
                        }else{
                            vmHome.insertSecretData(EntityTotp(0,data.name,data.secretBase32))
                        }
                        println("Account Name: ${data.name}, issuer: ${data.issuer}, Secret: ${data.secretBase32}")
                    }
                }else {
                    showCustomToast("Something went wrong, please try again")
                }
            }

        } catch (e: Exception) {
            showCustomToast("Error parsing TOTP URI: ${e.localizedMessage}")
        }
    }

    //Bottom Sheet
    private fun showBottomSheetDialog(onCameraClick:() -> Unit, onGalleryClick: () -> Unit) {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val binding = BottomSheetLayoutBinding.inflate(LayoutInflater.from(requireContext()))
        bottomSheetDialog.setContentView(binding.root)

        binding.cameraLayout.setOnClickListener {
            onCameraClick.invoke()
            bottomSheetDialog.dismiss()
        }
        binding.galleryLayout.setOnClickListener {
            onGalleryClick.invoke()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}