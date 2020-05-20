package org.rfcx.audiomoth.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import org.rfcx.audiomoth.R

class CameraPermissions(private val activity: Activity) {

    private var onCompletionCallback: ((Boolean) -> Unit)? = null

    fun allowed(): Boolean {
        val permissionCameraState = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        val permissionStorageState = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return permissionCameraState == PackageManager.PERMISSION_GRANTED && permissionStorageState == PackageManager.PERMISSION_GRANTED
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

    fun handleRequestResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS_IMAGE_CAPTURE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                val shouldProvideCamera = ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.CAMERA)
                val shouldProvideStorage = ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (!shouldProvideCamera || !shouldProvideStorage) {
                    val dialogBuilder: AlertDialog.Builder =
                        AlertDialog.Builder(activity).apply {
                            setTitle(null)
                            setMessage(R.string.camera_permission_msg)
                            setPositiveButton(R.string.go_to_setting) { _, _ ->
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${activity.packageName}"))
                                intent.addCategory(Intent.CATEGORY_DEFAULT)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                activity.startActivity(intent)
                            }
                        }
                    dialogBuilder.create().show()
                } else {
                    onCompletionCallback?.invoke(false)
                }
            } else {
                onCompletionCallback?.invoke(true)
            }
        }
    }

    companion object {
        const val REQUEST_PERMISSIONS_IMAGE_CAPTURE = 4000
    }
}