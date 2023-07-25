package org.rfcx.companion.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class NotificationPermissions(private val activity: Activity) {
    private var onCompletionCallback: ((Boolean) -> Unit)? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun notificationAllowed(): Boolean {
        val permissionState =
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun check(onCompletionCallback: (Boolean) -> Unit) {
        this.onCompletionCallback = onCompletionCallback
        if (!notificationAllowed()) {
            request()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun request() {
        activity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS
        )
    }

    fun handleRequestResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                onCompletionCallback?.invoke(true)
            }
        }
    }

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 36
    }
}
