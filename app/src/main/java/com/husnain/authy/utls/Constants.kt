package com.husnain.authy.utls

object Constants {
    const val SIGNUPTOPIN_KEY = "key_signup_to_pin"
    const val SYNC_CHANNEL_ID = "syncChannel"
    const val KEY_IS_COMING_FROM_SETTINGS_FOR_GOOGLE_AUTH_IMPORT = "isComingFromSetting"
    const val TAG = "LOG_AUTHY"
    var isComingToAuthFromGuest = false
    var isComingToAuthFromGuestToSignIn = false
    //Warning! don't change ids until unless changes from console
    const val weaklySubId = "weekly_plan"
    const val monthlySubId = "monthly_plan"
    const val lifeTimePorductId = "lifetime_pro"
    var isComingAfterRestore = false
}