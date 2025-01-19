package com.husnain.authy.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.husnain.authy.data.models.ModelPurchase
import com.husnain.authy.data.models.ModelUser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        //Keys
        private const val PREF_NAME = "MyPrefs"
        private const val KEY_ONBOARDING_FINISHED = "key_onboarding_finished"
        private const val KEY_ALLOW_SCREEN_SHOTS = "key_allow_screen_shots"
        private const val KEY_PIN = "keyPin"
        private const val KEY_LANG = "kayLang"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
        private const val KEY_USER = "key_user"
        private const val KEY_IS_SUBSCRIPTION_ACTIVE = "keyIsSubscriptionActive"
        private const val KEY_SUBSCRIPTION_END_DATE = "keySubscriptionEndDate"
    }

    private val gson = Gson()

    private val myPref: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveOnboardingFinished(isFinished: Boolean) {
        myPref.edit().apply {
            putBoolean(KEY_ONBOARDING_FINISHED, isFinished)
            apply()
        }
    }

    fun isOnboardingFinished(): Boolean {
        return myPref.getBoolean(KEY_ONBOARDING_FINISHED, false)
    }

    fun saveIsAllowScreenShots(allow: Boolean) {
        myPref.edit().apply {
            putBoolean(KEY_ALLOW_SCREEN_SHOTS, allow)
            apply()
        }
    }

    fun isAllowScreenShots(): Boolean {
        return myPref.getBoolean(KEY_ALLOW_SCREEN_SHOTS, false)
    }

    fun savePin(pin: String) {
        myPref.edit().apply {
            putString(KEY_PIN, pin)
            apply()
        }
    }

    fun getPin(): String? {
        return myPref.getString(KEY_PIN, "")
    }

    fun saveLang(lang: String) {
        myPref.edit().apply {
            putString(KEY_LANG, lang)
            apply()
        }
    }

    fun getLang(): String? {
        return myPref.getString(KEY_LANG, "")
    }

    fun saveBiometricLock(isEnabled: Boolean) {
        myPref.edit().putBoolean(KEY_BIOMETRIC_LOCK, isEnabled).apply()
    }

    fun isBiometricLockEnabled(): Boolean {
        return myPref.getBoolean(KEY_BIOMETRIC_LOCK, false)
    }

    fun saveUserData(user: ModelUser) {
        val json = gson.toJson(user)
        myPref.edit().putString(KEY_USER, json).apply()
    }

    fun getUserData(): ModelUser? {
        val json = myPref.getString(KEY_USER, null)
        return gson.fromJson(json, ModelUser::class.java)
    }

    fun saveSubscriptionActive(logedIn: Boolean) {
        myPref.edit().putBoolean(KEY_IS_SUBSCRIPTION_ACTIVE, logedIn).apply()
    }

    fun isSubscriptionActive(): Boolean {
        return myPref.getBoolean(KEY_IS_SUBSCRIPTION_ACTIVE, false)
    }

}