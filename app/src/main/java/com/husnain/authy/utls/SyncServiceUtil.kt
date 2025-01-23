package com.husnain.authy.utls

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.fragment.main.backup.workers.SyncJobService

object SyncServiceUtil {
    private fun startSyncJob(context: Context ,userId: String) {
        val jobScheduler = context.getSystemService(JobScheduler::class.java)
        val extras = PersistableBundle().apply {
            putString("userId", userId)
        }

        val jobInfo = JobInfo.Builder(123, ComponentName(context, SyncJobService::class.java))
            .setExtras(extras)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .build()

        jobScheduler.schedule(jobInfo)
    }

    fun syncIfUserValidForSyncing(context: Context,userId:String?,preferenceManager: PreferenceManager){
        val isUserLoggedIn = userId != null
        when{
            preferenceManager.isSubscriptionActive() && isUserLoggedIn -> userId?.let {
                startSyncJob(context,
                    it
                )
            }
            else -> return
        }
    }
}