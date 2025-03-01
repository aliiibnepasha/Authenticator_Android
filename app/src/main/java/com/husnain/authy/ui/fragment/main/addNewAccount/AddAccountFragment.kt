package com.husnain.authy.ui.fragment.main.addNewAccount

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
import androidx.camera.core.Camera
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.husnain.authy.R
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.FragmentAddAccountBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.DataState
import com.husnain.authy.utls.Flags
import com.husnain.authy.utls.OtpMigration
import com.husnain.authy.utls.QRCodeAnalyzer
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.invisible
import com.husnain.authy.utls.navigate
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject


@AndroidEntryPoint
@Suppress("DEPRECATION")
class AddAccountFragment : Fragment() {
    private var _binding: FragmentAddAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraExecutor: ExecutorService
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var camera: Camera
    private var isFlashOn = false
    private val vmAddAccount: VmAddAccount by viewModels()
    private var isComingFromSetting = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO){
            inItUiAndCamera()
            startCamera()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAccountBinding.inflate(inflater, container, false)
        binding.customOverlay.setFrameReference(binding.linearLayout)
        inIt()
        return binding.root
    }

    private fun inIt() {
        requestPermission()
        setOnClickListener()
        getBundleDataAndSetUi()
        setUpObservers()
    }

    private fun setOnClickListener() {
        binding.btnFlash.setOnClickListener {
            if (::camera.isInitialized) {
                isFlashOn = !isFlashOn  // Toggle state
                camera.cameraControl.enableTorch(isFlashOn)
                if (isFlashOn){
                    binding.btnFlash.setImageResource(R.drawable.ic_btn_off_flash_light)
                }else{
                    binding.btnFlash.setImageResource(R.drawable.ic_btn_on_flash_light)
                }
            } else {
                showCustomToast(getString(R.string.camera_not_initialized_yet))
            }
        }

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

    private fun getBundleDataAndSetUi() {
        val value =
            arguments?.getBoolean(Constants.KEY_IS_COMING_FROM_SETTINGS_FOR_GOOGLE_AUTH_IMPORT)
        value?.let { isValue ->
            isComingFromSetting = isValue
            binding.btnAddAccountManually.apply { if (isValue) invisible() else visible() }
            binding.btnGallery.apply { if (isValue) invisible() else visible() }
        }
    }

    private fun requestPermission() {
        requestCameraPermissionIfMissing { granted ->
            if (granted) {
                lifecycleScope.launch(Dispatchers.IO){
                    inItUiAndCamera()
                    startCamera()
                }
            }
        }
    }

    private fun requestCameraPermissionIfMissing(onResult: ((Boolean) -> Unit)) {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
        } else {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { onResult(it) }.launch(
                CAMERA
            )
        }
    }

    private fun setUpObservers() {
        vmAddAccount.insertState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DataState.Loading -> {
                }

                is DataState.Success -> {
                    Flags.isComingAfterAddingTotpData = true
                    navigate(R.id.action_addAccountFragment_to_homeFragment)
                }

                is DataState.Error -> {
                    showCustomToast("Error: ${state.message}")
                }
            }
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

    private fun logQRCodeScanned() {
        val bundle = Bundle().apply {
            putString("scan_result", "success")
        }
        firebaseAnalytics.logEvent("qr_code_scanned", bundle)
    }


    private fun handleQrContent(qrContent: String?) {
        if (qrContent != null) {
//            logQRCodeScanned()
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
                            showCustomToast(getString(R.string.qr_code_scan_failed))
                        }
                    )
                )
            }

        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        camera = cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalysis)

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

    //lifecycle callbacks
    override fun onResume() {
        super.onResume()
        makeFragmentFullScreen()
    }

    private fun getFlashCameraId(cameraManager: CameraManager): String? {
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
            val isBackCamera = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            if (hasFlash && isBackCamera) {
                return id
            }
        }
        return null
    }

    private fun toggleFlashlight(context: Context, isOn: Boolean) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = getFlashCameraId(cameraManager) ?: return // Get correct back camera ID

        try {
            cameraManager.setTorchMode(cameraId, isOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onPause() {
        super.onPause()
//        lifecycleScope.launch {
//            if (::cameraProvider.isInitialized){
//                cameraProvider.unbindAll()
//            }
//        }
        if (::cameraExecutor.isInitialized) {
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
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        _binding = null
    }
}
