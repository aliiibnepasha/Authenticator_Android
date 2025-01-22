package com.husnain.authy.ui.fragment.main.backup.workers

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.husnain.authy.data.room.SyncDatabase
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.preferences.PreferenceManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncJobService : JobService() {
    private lateinit var daoTotp: DaoTotp
    private lateinit var firestore: FirebaseFirestore
    private lateinit var notificationManager: SyncNotificationHelper

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartJob(params: JobParameters?): Boolean {
        daoTotp = SyncDatabase.getDatabase(applicationContext).daoTotp()
        firestore = FirebaseFirestore.getInstance()
        val preferenceManager = PreferenceManager(applicationContext)
        notificationManager = SyncNotificationHelper(applicationContext)

        GlobalScope.launch(Dispatchers.IO) {

            val userId = params?.extras?.getString("userId") ?: return@launch

            try {
                val totpList = daoTotp.getAllTotpData()

                if (totpList.isEmpty()) {
                    jobFinished(params, false)
                    return@launch
                }

                notificationManager.showStartNotification()
                val totpsCollectionRef = firestore.collection("totps").document(userId).collection("totps")

                totpList.forEachIndexed { index, totp ->
                    val existingDocs = totpsCollectionRef
                        .whereEqualTo("secretKey", totp.secretKey) // Query for existing document
                        .get()
                        .await()

                    if (existingDocs.isEmpty) {
                        // Add new document
                        val totpData = hashMapOf(
                            "uid" to totp.uid,
                            "serviceName" to totp.serviceName,
                            "secretKey" to totp.secretKey
                        )
                        notificationManager.updateProgress(index + 1, totpList.size)
                        totpsCollectionRef.add(totpData).await()
                    } else {
                        Log.d("SyncJobService", "Skipping duplicates")
                    }

                }

                preferenceManager.saveLastSyncDateTime()
                notificationManager.showCompletionNotification()
                jobFinished(params, false)
            } catch (e: Exception) {
                notificationManager.showErrorNotification(e.message ?: "Sync failed")
                jobFinished(params, false)
            }
        }

        return true // Return true because the job is running asynchronously
    }


    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }
}
