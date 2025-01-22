package com.husnain.authy.app

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.akexorcist.localizationactivity.ui.LocalizationApplication
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.fragment.main.backup.workers.SyncJobService
import com.husnain.authy.utls.Constants
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
        inItNotificationChannel()
        setupActivityListener()
        isScreenshotRestricted = preferenceManager.isAllowScreenShots()
    }

    private fun inItNotificationChannel() {
        val channelId = Constants.SYNC_CHANNEL_ID
        val channelName = "Sync Notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Channel for sync notifications"
        }

        channel.setSound(null, null);
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    private fun scheduleSyncJob() {
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val networkType = JobInfo.NETWORK_TYPE_ANY  // Run on either Wi-Fi or mobile data

        val jobInfo = JobInfo.Builder(1, ComponentName(this, SyncJobService::class.java))
            .setRequiredNetworkType(networkType)  // Specify network condition (Wi-Fi or mobile data)
            .setPersisted(true)  // Persist across reboots
            .setPeriodic(15 * 60 * 1000)  // Sync every 15 minutes
            .build()

        // Check if network is available before scheduling the job
        if (isNetworkAvailable()) {
            jobScheduler.schedule(jobInfo)
            Log.d("SyncJob", "Job scheduled")
        } else {
            Log.d("SyncJob", "No network available, job not scheduled")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
