package com.husnain.authy.ui.fragment.main.addNewAccount

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.husnain.authy.R
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.FragmentAddAccountBinding
import com.husnain.authy.ui.fragment.main.home.VmHome
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.OtpMigration
import com.husnain.authy.utls.QRCodeAnalyzer
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
@Suppress("DEPRECATION")
class AddAccountFragment : Fragment() {
    private var _binding: FragmentAddAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraExecutor: ExecutorService
    private val vmHome: VmHome by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAccountBinding.inflate(inflater, container, false)
        inItUiAndCamera()
        requestCameraPermissionIfMissing { granted ->
            if (granted) {
                startCamera()
            }
        }
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
        setUpObservers()
    }

    override fun onResume() {
        super.onResume()
        makeFragmentFullScreen()
    }

    private fun requestCameraPermissionIfMissing(onResult: ((Boolean) -> Unit)) {
        if (ContextCompat.checkSelfPermission(requireActivity(), CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
        } else {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { onResult(it) }.launch(CAMERA)
        }
    }

    private fun setOnClickListener() {
        binding.buttonClose.setOnClickListener {
            popBack()
        }

        binding.btnGallery.setOnClickListener {
            openGallery()
        }

        binding.btnAddAccountManually.setOnClickListener {
            navigate(R.id.action_addAccountFragment_to_addAccountManuallyFragment)
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
                    vmHome.insertSecretData(EntityTotp(0, label, secretKey))
                } else {
                    showCustomToast(getString(R.string.string_something_went_wrong_please_try_again))
                }
            } else {
                val dataUri = uri.replace("otpauth-migration://offline?data=", "")
                val decodedDataList = OtpMigration.getGoogleAuthData(dataUri)

                if (decodedDataList != null) {
                    for (data in decodedDataList) {
                        if (data.name.isEmpty() && data.issuer.isNotEmpty()) {
                            vmHome.insertSecretData(EntityTotp(0, data.issuer, data.secretBase32))
                        } else {
                            vmHome.insertSecretData(EntityTotp(0, data.name, data.secretBase32))
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

    private fun setUpObservers() {
        vmHome.insertState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    navigate(R.id.action_addAccountFragment_to_homeFragment)
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
        }
    }

    // Camera and ML
    private fun inItUiAndCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                return@addListener
            }
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))

    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = binding.previewView.surfaceProvider
        }

        val barcodeScanner = BarcodeScanning.getClient()
        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                ).build()
            ).build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    QRCodeAnalyzer(
                        barcodeScanner,
                        onSuccess = { uri ->
                            onQRCodeScanned(uri)
                        },
                        onError = {
                            showCustomToast("QR Code scan failed.")
                        }
                    )
                )
            }

        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalysis)
    }

    private fun onQRCodeScanned(value: String) {
        handleQrContent(value)
    }

    private fun makeFragmentFullScreen() {
        requireActivity().window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onPause() {
        super.onPause()
//        lifecycleScope.launch {
//            if (::cameraProvider.isInitialized){
//                cameraProvider.unbindAll()
//            }
//        }
        if (::cameraExecutor.isInitialized){
            cameraExecutor.shutdown()
        }
        requireActivity().window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE // Reset the system UI visibility
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::cameraExecutor.isInitialized){
            cameraExecutor.shutdown()
        }
        _binding = null
    }
}
