package com.husnain.authy.ui.fragment.main.backup.workers

import android.app.job.JobParameters
import android.app.job.JobService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
                val otpList = daoTotp.getAllTotpData()

                if (otpList.isEmpty()) {
                    jobFinished(params, false)
                    return@launch
                }

                notificationManager.showStartNotification()
                val totpsCollectionRef = firestore.collection("totps").document(userId).collection("totps")

                otpList.forEachIndexed { index, totp ->
                    val totpData = hashMapOf(
                        "uid" to totp.uid,
                        "serviceName" to totp.serviceName,
                        "secretKey" to totp.secretKey
                    )

                    val documentId = totp.secretKey // Use secretKey as the document ID
                    notificationManager.updateProgress(index + 1, otpList.size)

                    // Set the document with the specified ID
                    totpsCollectionRef.document(documentId).set(totpData, SetOptions.merge()).await()
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
