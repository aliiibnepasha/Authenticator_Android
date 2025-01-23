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
        private const val KEY_ONBOARDING_FINISHED = "key_onboarding_finished"
        private const val KEY_ALLOW_SCREEN_SHOTS = "key_allow_screen_shots"
        private const val KEY_PIN = "keyPin"
        private const val KEY_LANG = "kayLang"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
        private const val KEY_USER = "key_user"
        private const val KEY_IS_SUBSCRIPTION_ACTIVE = "keyIsSubscriptionActive"
        private const val KEY_IS_LIFE_TIME_ACCESS_ACTIVE = "lifeTimeAccess"
        private const val KEY_LAST_APP_OPEN_TIME = "last_app_open_time"
        private const val kEY_GUEST_USER = "keyGuestUser"
        private const val KEY_SETTING_SCROLL_POSITION = "keySettingScrollPosition"
        private const val KEY_LAST_SYNC_TIME = "lastSyncTime"
        private const val KEY_IS_TO_SHOW_SUBS_SCREEN = "showSubsScreen"
        private const val kEY_SUBSCRIPTION_DATA_LIST = "mySubsDataList"
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

    fun saveLifeTimeAccessActive(isLifeTimeAccess: Boolean) {
        myPref.edit().putBoolean(KEY_IS_LIFE_TIME_ACCESS_ACTIVE, isLifeTimeAccess).apply()
    }

    fun isLifeTimeAccessActive(): Boolean {
        return myPref.getBoolean(KEY_IS_LIFE_TIME_ACCESS_ACTIVE, false)
    }

    fun saveGuestUser(isGuest: Boolean) {
        myPref.edit().putBoolean(kEY_GUEST_USER, isGuest).apply()
    }

    fun isGuestUser(): Boolean {
        return myPref.getBoolean(kEY_GUEST_USER, true)
    }

    fun saveDelayOption(option: DelayOption) {
        myPref.edit().putString("selected_delay_option", option.name).apply()
    }

    fun getDelayOption(): DelayOption {
        val name = myPref.getString("selected_delay_option", DelayOption.NEVER.name)
        return DelayOption.valueOf(name!!)
    }

    fun saveLastAppOpenTime() {
        val currentTime = System.currentTimeMillis()
        myPref.edit().putLong(KEY_LAST_APP_OPEN_TIME, currentTime).apply()
    }

    fun getLastAppOpenTime(): Long {
        return myPref.getLong(KEY_LAST_APP_OPEN_TIME, 0)
    }

    fun saveSettingScrollPosition(position: Int) {
        myPref.edit().putInt(KEY_SETTING_SCROLL_POSITION, position).apply()
    }

    fun getSettingScrollPosition(): Int {
        return myPref.getInt(KEY_SETTING_SCROLL_POSITION, 0)
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

    fun saveSubscriptionDataListToPrefs(list: List<ModelSubscription>) {
        val json = gson.toJson(list)
        myPref.edit().putString(kEY_SUBSCRIPTION_DATA_LIST, json).apply()
    }

    fun getSubscriptionDataList(): List<ModelSubscription>? {
        val json = myPref.getString(kEY_SUBSCRIPTION_DATA_LIST, null)
        return if (json != null) {
            val type = object : TypeToken<List<ModelSubscription>>() {}.type
            gson.fromJson<List<ModelSubscription>>(json, type)
        } else {
            null
        }
    }

}