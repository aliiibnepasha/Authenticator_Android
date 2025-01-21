package com.husnain.authy.utls

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.husnain.authy.R
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.BottomSheetLayoutBinding

fun handleTOTPURI(
    uri: String,
    onInsertSecret: (EntityTotp) -> Unit,
    onError: (String) -> Unit = { error ->
        Log.e("TOTPHandler", "Error: $error")
    },
) {
    try {
        if (uri.startsWith("otpauth://")) {
            val uriObject = Uri.parse(uri)
            val secretKey = uriObject.getQueryParameter("secret")

            val path = uriObject.path?.removePrefix("/")
            val label = path?.substringAfter("totp/")?.substringBefore(":") ?: path?.substringAfter("totp/")

            if (secretKey != null && label != null) {
                val entity = EntityTotp(0, label, secretKey)
                onInsertSecret(entity)
            } else {
                onError("Invalid TOTP URI format")
            }
        } else {
            val data = uri.replace("otpauth-migration://offline?data=", "")
            val decodedDataList = OtpMigration.getGoogleAuthData(data)

            if (decodedDataList != null) {
                for (data in decodedDataList) {
                    val label = if (data.name.isEmpty() && data.issuer.isNotEmpty()) {
                        data.issuer
                    } else {
                        data.name
                    }
                    val entity = EntityTotp(0, label, data.secretBase32)
                    onInsertSecret(entity)
                }
            } else {
                onError("Failed to decode Google Auth data")
            }
        }
    } catch (e: Exception) {
        onError("Error parsing TOTP URI: ${e.localizedMessage}")
    }
}

fun Fragment.showBottomSheetDialog(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
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


fun Fragment.setupGalleryPicker(
    onImagePicked: (Uri) -> Unit
): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                onImagePicked(uri)
            }
        }
    }
}

fun Fragment.openGallery(launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "image/*"
    }
    launcher.launch(Intent.createChooser(intent, "Select Image"))
}

fun Context.decodeQRCode(imageUri: Uri, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
    try {
        // Open input stream to read the image
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Convert the bitmap to an int array
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        // Process the image using ZXing
        val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = QRCodeReader()
        val result = reader.decode(binaryBitmap)

        // Invoke success callback with the QR code content
        onSuccess(result.text)
    } catch (e: Exception) {
        // Invoke error callback
        onError(e)
    }
}


