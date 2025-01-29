package com.husnain.authy.app

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.akexorcist.localizationactivity.ui.LocalizationApplication
import com.google.android.gms.ads.MobileAds
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.ui.fragment.main.subscription.SubscriptionFragment
import com.husnain.authy.utls.Constants
import com.husnain.authy.utls.admob.AppOpenAdManager
import dagger.hilt.android.HiltAndroidApp
import java.lang.ref.WeakReference
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App : LocalizationApplication(){
    lateinit var appOpenAdManager: AppOpenAdManager
    private val activityList: MutableList<Activity> = mutableListOf()
    var isScreenshotRestricted: Boolean = false
    private var currentActivity: WeakReference<MainActivity>? = null
    var isFreshStart: Boolean = true
    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun getDefaultLanguage(context: Context): Locale = Locale.ENGLISH

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleListener())

        inItNotificationChannel()
        setupActivityListener()
        isScreenshotRestricted = preferenceManager.isAllowScreenShots()

        //Admob
        MobileAds.initialize(this@App) {}
        appOpenAdManager = AppOpenAdManager(this)
        if (!preferenceManager.isSubscriptionActive()){
            appOpenAdManager.loadAd()
        }
    }

    fun registerMainActivity(activity: MainActivity) {
        currentActivity = WeakReference(activity)
    }

    fun unregisterMainActivity() {
        currentActivity = null
    }

    /**
     * AppLifecycleListener is responsible for observing the app's lifecycle events.
     * It manages actions based on whether the app is starting fresh or coming back from the background.
     *
     * - onStart: Checks if the app is starting fresh or resuming from the background. If not fresh, it shows the app open ad
     *   if conditions (e.g., subscription not active) are met.
     * - onStop: Loads a new app open ad when the app is sent to the background.
     */

    inner class AppLifecycleListener : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            Log.d(Constants.TAG, "isFreshStart = $isFreshStart")
            if (isFreshStart){
                isFreshStart = false
                return
            }
            currentActivity?.get()?.let { mainActivity ->
                if (!mainActivity.preferenceManager.isSubscriptionActive() &&
                    mainActivity.navHostFragment.isAdded &&
                    mainActivity.navHostFragment.childFragmentManager.fragments.isNotEmpty() &&
                    mainActivity.navHostFragment.childFragmentManager.fragments.first() !is SubscriptionFragment
                ) {
                    mainActivity.runOnUiThread {
                        appOpenAdManager.showAdIfAvailableFromFragment(mainActivity) {
                            appOpenAdManager.loadAd()
                        }
                    }
                }
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            Log.d(Constants.TAG, "background")
        }
    }
    //Admob open app ad

    private fun inItNotificationChannel() {
        val channelId = Constants.SYNC_CHANNEL_ID
        val channelName = "Sync Notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Channel for sync notifications"
        }

        channel.setSound(null, null);
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
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
