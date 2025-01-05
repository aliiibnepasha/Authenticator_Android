package com.husnain.authy.utls

import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import java.util.Date
import java.util.concurrent.TimeUnit

object TotpUtil {
    private const val TOTP_PERIOD_SECONDS = 30

    private fun createTotpGenerator(secretKey: String): GoogleAuthenticator {
        return GoogleAuthenticator(secretKey.toByteArray())
    }

    fun getRemainingSeconds(): Int {
        val currentTimeMillis = System.currentTimeMillis()
        return (TOTP_PERIOD_SECONDS - ((currentTimeMillis / 1000) % TOTP_PERIOD_SECONDS)).toInt()
    }

    fun generateTotp(secretKey: String): String {
        val googleAuthenticator = createTotpGenerator(secretKey)
        val timestamp = Date(System.currentTimeMillis())
        return googleAuthenticator.generate(timestamp)
    }
}