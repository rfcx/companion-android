package org.rfcx.companion.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class CameraPermissions(private val activity: Activity) {

    private var onCompletionCallback: ((Boolean) -> Unit)? = null

    fun allowed(): Boolean {
        val permissionCameraState = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val permissionStorageState =
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return permissionCameraState == PackageManager.PERMISSION_GRANTED && permissionStorageState == PackageManager.PERMISSION_GRANTED
        }
        return permissionCameraState == PackageManager.PERMISSION_GRANTED
    }

    fun check(onCompletionCallback: (Boolean) -> Unit) {
        this.onCompletionCallback = onCompletionCallback
        if (!allowed()) {
            request()
        }
    }

    private fun request() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSIONS_IMAGE_CAPTURE)
        } else {
            throw Exception("Request permissions not required before API 23 (should never happen)")
        }
    }

    companion object {
        const val REQUEST_PERMISSIONS_IMAGE_CAPTURE = 4000
    }
}
