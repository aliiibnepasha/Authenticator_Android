package com.husnain.authy.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import com.akexorcist.localizationactivity.ui.LocalizationApplication
import com.husnain.authy.preferences.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App : LocalizationApplication() {

    private val activityList: MutableList<Activity> = mutableListOf()
    var isScreenshotRestricted: Boolean = false
    @Inject lateinit var preferenceManager: PreferenceManager

    override fun getDefaultLanguage(context: Context): Locale = Locale.ENGLISH
    override fun onCreate() {
        super.onCreate()
        setupActivityListener()
        isScreenshotRestricted = preferenceManager.isAllowScreenShots()
    }

    private fun setupActivityListener() {
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activityList.add(activity)

                if (isScreenshotRestricted) {
                    activity.window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                activityList.remove(activity)
            }
        })
    }

    fun setScreenshotRestriction(restricted: Boolean) {
        isScreenshotRestricted = restricted

        for (activity in activityList) {
            if (restricted) {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

}
