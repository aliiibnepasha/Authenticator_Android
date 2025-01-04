package com.husnain.authy.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        //Keys
        private const val PREF_NAME = "MyPrefs"
        private const val KEY_ONBOARDING_FINISHED = "key_onboarding_finished"
        private const val KEY_ALLOW_SCREEN_SHOTS = "key_allow_screen_shots"
    }


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

}