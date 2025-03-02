package com.husnain.authy.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.husnain.authy.data.models.ModelSubscription
import com.husnain.authy.data.models.ModelUser
import com.husnain.authy.utls.DelayOption
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        //Keys
        private const val PREF_NAME = "MyPrefs"
        private const val KEY_IS_FIRST_LOGIN = "isFirstLogin"
        private const val KEY_ONBOARDING_FINISHED = "key_onboarding_finished"
        private const val KEY_ALLOW_SCREEN_SHOTS = "key_allow_screen_shots"
        private const val KEY_PIN = "keyPin"
        private const val KEY_LANG = "kayLang"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
        private const val KEY_USER = "key_user"
        private const val KEY_IS_SUBSCRIPTION_ACTIVE = "keyIsSubscriptionActive"
        private const val KEY_IS_LIFE_TIME_ACCESS_ACTIVE = "lifeTimeAccess"
        private const val KEY_LAST_APP_OPEN_TIME = "last_app_open_time"
        private const val KEY_GUEST_USER = "keyGuestUser"
        private const val KEY_LAST_SYNC_TIME = "lastSyncTime"
        private const val KEY_IS_TO_SHOW_SUBS_SCREEN = "showSubsScreen"
        private const val KEY_IS_FIRST_INSTALL = "isFirstInstall"
        private const val KEY_OPEN_COUNT = "open_count"
        private const val KEY_HOME_BANNER_AD = "key_home_banner_ad"
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

    fun saveIsToShowSubsScreenAsDialog(isToShow: Boolean) {
        myPref.edit().apply {
            putBoolean(KEY_IS_TO_SHOW_SUBS_SCREEN, isToShow)
            apply()
        }
    }

    fun isToShowSubsScreenAsDialog(): Boolean {
        return myPref.getBoolean(KEY_IS_TO_SHOW_SUBS_SCREEN, false)
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
        return myPref.getString(KEY_LANG, "en")
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

    fun saveSubscriptionActive(isActive: Boolean) {
        if (!isActive){
            saveLifeTimeAccessActive(false)
        }
        myPref.edit().putBoolean(KEY_IS_SUBSCRIPTION_ACTIVE, isActive).apply()
    }

    fun isSubscriptionActive(): Boolean {
        return myPref.getBoolean(KEY_IS_SUBSCRIPTION_ACTIVE, false)
    }

    fun saveIsSyncOn(isActive: Boolean) {
        myPref.edit().putBoolean("isSync", isActive).apply()
    }

    fun isSyncOn(): Boolean {
        return myPref.getBoolean("isSync", false)
    }

    fun saveLifeTimeAccessActive(isLifeTimeAccess: Boolean) {
        myPref.edit().putBoolean(KEY_IS_LIFE_TIME_ACCESS_ACTIVE, isLifeTimeAccess).apply()
    }

    fun isLifeTimeAccessActive(): Boolean {
        return myPref.getBoolean(KEY_IS_LIFE_TIME_ACCESS_ACTIVE, false)
    }

    fun saveGuestUser(isGuest: Boolean) {
        myPref.edit().putBoolean(KEY_GUEST_USER, isGuest).apply()
    }


    fun isGuestUser(): Boolean {
        return myPref.getBoolean(KEY_GUEST_USER, true)
    }

    fun saveIsFirstLoginAfterAppInstall(isGuest: Boolean) {
        myPref.edit().putBoolean(KEY_IS_FIRST_LOGIN, isGuest).apply()
    }

    fun isFirstLoginAfterAppInstall(): Boolean {
        return myPref.getBoolean(KEY_IS_FIRST_LOGIN, false)
    }

    fun saveIsFirstTimeOnApp(isFirstTime: Boolean) {
        myPref.edit().putBoolean(KEY_IS_FIRST_INSTALL, isFirstTime).apply()
    }

    fun isFirstTimeOnApp(): Boolean {
        return myPref.getBoolean(KEY_IS_FIRST_INSTALL, true)
    }

    fun saveDelayOption(option: DelayOption) {
        myPref.edit().putString("selected_delay_option", option.name).apply()
    }

    fun getDelayOption(): DelayOption {
        val name = myPref.getString("selected_delay_option", DelayOption.IMMEDIATELY.name)
        return DelayOption.valueOf(name!!)
    }

    fun saveLastAppOpenTime() {
        val currentTime = System.currentTimeMillis()
        myPref.edit().putLong(KEY_LAST_APP_OPEN_TIME, currentTime).apply()
    }

    fun getLastAppOpenTime(): Long {
        return myPref.getLong(KEY_LAST_APP_OPEN_TIME, 0)
    }

    fun saveLastSyncDateTime() {
        val currentTime = System.currentTimeMillis()
        val formatter = SimpleDateFormat("d MMM yyyy - hh:mm a", Locale.ENGLISH)
        val formattedTime = formatter.format(Date(currentTime))
        myPref.edit().putString(KEY_LAST_SYNC_TIME, formattedTime).apply()
    }

    fun getLastSyncTime(): String? {
        return myPref.getString(KEY_LAST_SYNC_TIME, "")
    }

    fun saveSubscriptionData(subscriptionList: List<ModelSubscription>) {
        val json = gson.toJson(subscriptionList)
        myPref.edit().putString("subscription_data", json).apply()
    }

    fun getSubscriptionData(): List<ModelSubscription> {
        val json = myPref.getString("subscription_data", null)
        return if (json != null) {
            val type = object : TypeToken<List<ModelSubscription>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getOpenCount(): Int {
        return myPref.getInt(KEY_OPEN_COUNT, 0)
    }

    fun incrementOpenCount() {
        val currentCount = myPref.getInt(KEY_OPEN_COUNT, 0)
        myPref.edit().putInt(KEY_OPEN_COUNT, currentCount + 1).apply()
    }

    fun saveHomeBannerAd(isEnabled: Boolean) {
        myPref.edit().putBoolean(KEY_HOME_BANNER_AD, isEnabled).apply()
    }

    fun isHomeBannerAdEnabled(): Boolean {
        return myPref.getBoolean(KEY_HOME_BANNER_AD, true) // Default to true if not set
    }

    fun saveLangSmall(isSmall: Boolean) {
        myPref.edit().putBoolean("islangscreenadsmall", isSmall).apply()
    }

    fun isLangScreenAdSmallConfig(): Boolean {
        return myPref.getBoolean("islangscreenadsmall", true) // Default to true if not set
    }
}