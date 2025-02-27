package com.husnain.authy.utls

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import android.util.Log

object RemoteConfigUtil {
    private const val HOME_NATIVE_BANNER_AD = "HomeNativeBannerAd"
    private const val NATIVE_AD_LANGUAGE = "NativeAdLanguage"

    private val firebaseRemoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0) // Fetch every 1 hour
                .build()
            setConfigSettingsAsync(configSettings)
        }
    }

    fun fetchRemoteConfig(onComplete: (Boolean) -> Unit) {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("RemoteConfig", "Fetch successful: ${firebaseRemoteConfig.all}")
                onComplete(true)
            } else {
                Log.e("RemoteConfig", "Fetch failed: ${task.exception}")
                onComplete(false)
            }
        }
    }

    fun getHomeNativeBannerAd(): Int {
        return firebaseRemoteConfig.getLong(HOME_NATIVE_BANNER_AD).toInt()
    }

    fun getNativeAdLanguage(): Int {
        return firebaseRemoteConfig.getLong(NATIVE_AD_LANGUAGE).toInt()
    }
}
