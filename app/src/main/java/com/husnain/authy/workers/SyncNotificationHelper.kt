package com.husnain.authy.workers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.husnain.authy.R
import com.husnain.authy.utls.Constants

class SyncNotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showStartNotification() {
        val notification = NotificationCompat.Builder(context, Constants.SYNC_CHANNEL_ID)
            .setContentTitle("Sync")
            .setContentText("Syncing...")
            .setSmallIcon(R.drawable.img_baby_brain)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .build()

        notificationManager.notify(1, notification)
    }

    fun showCompletionNotification() {
        val notification = NotificationCompat.Builder(context, Constants.SYNC_CHANNEL_ID)
            .setContentTitle("Sync")
            .setContentText("Sync Completed")
            .setSmallIcon(R.drawable.img_baby_brain)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .build()

        notificationManager.notify(1, notification)
    }

    fun updateProgress(currentProgress: Int, total: Int) {
        val notification = NotificationCompat.Builder(context, Constants.SYNC_CHANNEL_ID)
            .setContentTitle("Sync")
            .setContentText("Sync progress: $currentProgress/$total")
            .setSmallIcon(R.drawable.img_baby_brain)
            .setProgress(total, currentProgress, false)
            .setSound(null)
            .build()

        notificationManager.notify(1, notification)
    }

    fun showErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(context, Constants.SYNC_CHANNEL_ID)
            .setContentTitle("Sync Failed")
            .setContentText("Something went wrong")
            .setSmallIcon(R.drawable.img_baby_brain)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }
}
