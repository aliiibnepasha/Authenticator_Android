package com.husnain.authy.utls

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import com.google.mlkit.vision.barcode.BarcodeScanner

class QRCodeAnalyzer(
    private val barcodeScanner: BarcodeScanner,
    private val onSuccess:(String) -> Unit,
    private val onError:() -> Unit
) :
    ImageAnalysis.Analyzer {
    @ExperimentalGetImage
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val value = barcode.rawValue
                        if (value != null) {
                            onSuccess.invoke(value)
                        }
                    }
                    imageProxy.close()
                }
                .addOnFailureListener {
                    onError.invoke()
                    imageProxy.close()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}