package com.husnain.authy.utls

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

object PermissionUtils{
    private const val MAX_PERMISSION_REQUESTS = 2
    private const val PERMISSION_PREF_NAME = "PermissionPrefs"
    private const val PERMISSION_REQUEST_COUNT_KEY = "permissionRequestCount"
    private const val PERMISSION_ALERT_TEXT = "Some permissions are still not granted. Please go to settings to enable them."

    private fun areAllPermissionsGranted(context: Context, permissions: Array<String> ) = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun handlePermissions(activity: Activity, permissions: Array<String>, requestCode: Int): Boolean {
        val sharedPreferences = activity.getSharedPreferences(PERMISSION_PREF_NAME, Context.MODE_PRIVATE)
        var permissionRequestCount = sharedPreferences.getInt(PERMISSION_REQUEST_COUNT_KEY, 0)
        return when {
            areAllPermissionsGranted(activity, permissions) -> true
            else -> {
                if (permissionRequestCount < MAX_PERMISSION_REQUESTS) {
                    sharedPreferences.edit()
                        .putInt(PERMISSION_REQUEST_COUNT_KEY, ++permissionRequestCount).apply()
                    requestPermissions(activity, permissions, requestCode)
                } else {
                    permissionAlertDialog(
                        activity,
                        PERMISSION_ALERT_TEXT
                    ) { openAppSettings(activity) }
                }
                false
            }
        }
    }

    private inline fun permissionAlertDialog(context: Context, message:String, crossinline callback:() -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.apply{
            setMessage(message)
            setPositiveButton("Yes") { dialog, _ ->
                callback.invoke()
                dialog.dismiss()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
            setCancelable(false)
            create()
            show()
        }
    }
}


