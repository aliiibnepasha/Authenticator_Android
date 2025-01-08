package com.husnain.authy.utls

import android.os.Build
import android.util.Base64
import android.util.Log
import com.google.authenticator.data.OtpMigration.Payload
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base32
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object OtpMigration {

    @JvmStatic
    fun getGoogleAuthData(encodedData: String): List<DecodedOtpData>? {
        try {
            // Step 1: URL Decode
            val urlDecodedData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                URLDecoder.decode(encodedData, StandardCharsets.UTF_8)
            } else {
                URLDecoder.decode(encodedData, "UTF-8")
            }

            Log.d("OtpDecoder", "URL Decoded: $urlDecodedData")

            // Step 2: Base64 Decode (using NO_WRAP)
            val fixedBase64 = urlDecodedData.replace("-", "+").replace("_", "/")
            val decodedBytes = Base64.decode(fixedBase64, Base64.NO_WRAP)
            Log.d("OtpDecoder", "Base64 Decoded Bytes: ${decodedBytes.joinToString(",") { it.toString() }}")

            // Step 3: Parse the Protocol Buffers data
            val payload = Payload.parseFrom(decodedBytes)
            val decodedDataList = mutableListOf<DecodedOtpData>()

            // Step 4: Extract and Log OTP Parameters
            for (otpParams in payload.otpParametersList) {
                val secret = otpParams.secret.toByteArray()
                val secretBase32 = toString(secret)
                val name = otpParams.name
                val issuer = otpParams.issuer
                val algo = otpParams.algorithm
                val digits = otpParams.digits
                val type = otpParams.type
                val counter = otpParams.counter

                Log.d("OtpDecoder", "Account Name: $name")
                Log.d("OtpDecoder", "Issuer: $issuer")
                Log.d("OtpDecoder", "Decoded OTP Secret (Base32): $secretBase32")
                Log.d("OtpDecoder", "Algorithm: $algo")
                Log.d("OtpDecoder", "Digits: $digits")
                Log.d("OtpDecoder", "type: $type")
                Log.d("OtpDecoder", "counter: $counter")


                decodedDataList.add(
                    DecodedOtpData(
                        secretBase32,
                        name,
                        issuer,
                        algo.toString(),
                        digits.toString(),
                        type.toString(),
                        counter
                    )
                )

            }
            return decodedDataList
        } catch (e: Exception) {
            Log.e("OtpDecoder", "Error decoding OTP data", e)
            return null
        }
    }
}

private fun toString(secret: ByteArray): String {
    val base32 = Base32()
    return base32.encodeToString(secret).trimEnd('=')
}

data class DecodedOtpData(
    val secretBase32:String,
    val name:String,
    val issuer:String,
    val algo:String,
    val digits:String,
    val type:String,
    val counter:Long
)

//package com.husnain.authy.utls
//
//import android.annotation.SuppressLint
//import android.os.Build
//import android.util.Base64
//import android.util.Log
//import com.google.authenticator.data.OtpMigration.Payload
//import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base32
//import java.net.URLDecoder
//import java.nio.charset.StandardCharsets
//
//object OtpMigration {
//    @JvmStatic
//    fun getGoogleAuthData(encodedData: String) {
//        try {
//
//            // Step 1: URL Decode
//            val urlDecodedData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                URLDecoder.decode(encodedData, StandardCharsets.UTF_8)
//            } else {
//                URLDecoder.decode(encodedData, "UTF-8")
//            }
//
//            Log.d("OtpDecoder", "URL Decoded: $urlDecodedData")
//
//            // Step 2: Base64 Decode (using NO_WRAP to handle more variations)
//            val fixedBase64 = urlDecodedData.replace("-", "+").replace("_", "/")
//            // val fixedBase64 = urlDecodedData.replace("-", "+").replace("_", "/")
//            val decodedBytes = Base64.decode(fixedBase64, Base64.NO_WRAP)
//            Log.d("OtpDecoder", "Base64 Decoded Bytes: ${decodedBytes.joinToString(",") { it.toString() }}")
//
//            // Step 3: Parse the Protocol Buffers data
//            val payload = Payload.parseFrom(decodedBytes)
//
//            // Step 4: Extract and Log OTP Parameters
//            for (otpParams in payload.otpParametersList) {
//                val secret = otpParams.secret.toByteArray()
//                Log.d("OtpDecoder", "OTP Secret Raw Bytes: ${secret.joinToString(",") { it.toString() }}")
//
//                val secretBase32 = toString(secret)
//
//                Log.d("OtpDecoder", "Decoded OTP Secret (Base32): $secretBase32")
//
//                val name = otpParams.name
//                Log.d("OtpDecoder", "Account Name: $name")
//
//                val issuer = otpParams.issuer
//                Log.d("OtpDecoder", "Issuer: $issuer")
//
//                val algo = otpParams.algorithm
//                Log.d("OtpDecoder", "Algorithm: $algo")
//
//                val digits = otpParams.digits
//                Log.d("OtpDecoder", "Digits: $digits")
//
//                val type = otpParams.type
//                Log.d("OtpDecoder", "type: $type")
//
//                val counter = otpParams.counter
//                Log.d("OtpDecoder", "counter: $counter")
//
//
//            }
//
//        } catch (e: Exception) {
//            Log.e("OtpDecoder", "Error decoding OTP data", e)
//        }
//    }
//}
//
//fun toString(secret: ByteArray): String {
//    val base32 = Base32()
//    return base32.encodeToString(secret).trimEnd('=')
//}