package com.husnain.authy.ui.fragment.main.backup.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.husnain.authy.R
import com.husnain.authy.utls.Constants

class SyncNotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showStartNotification() {
        val notification = NotificationCompat.Builder(context, Constants.SYNC_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.sync))
            .setContentText(context.getString(R.string.syncing))
            .setSmallIcon(R.drawable.img_baby_brain)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .build()

        notificationManager.notify(1, notification)
    }

    fun showCompletionNotification() {
        val notification = NotificationCompat.Builder(context, Constants.SYNC_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.sync))
            .setContentText(context.getString(R.string.sync_completed))
            .setSmallIcon(R.drawable.img_baby_brain)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .build()

        notificationManager.notify(1, notification)
    }

    fun updateProgress(currentProgress: Int, total: Int) {
        val notification = NotificationCompat.Builder(context, Constants.SYNC_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.sync))
            .setContentText("Sync progress: $currentProgress/$total")
            .setSmallIcon(R.drawable.img_baby_brain)
            .setProgress(total, currentProgress, false)
            .setSound(null)
            .build()

        notificationManager.notify(1, notification)
    }

    fun showErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(context, Constants.SYNC_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.sync_failed))
            .setContentText(context.getString(R.string.something_went_wrong))
            .setSmallIcon(R.drawable.img_baby_brain)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }
}
