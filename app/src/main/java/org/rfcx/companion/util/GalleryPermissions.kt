package org.rfcx.companion.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.rfcx.companion.R

class GalleryPermissions(val activity: Activity) {
    private var onCompletionCallback: ((Boolean) -> Unit)? = null

    fun allowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val permissionState = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
            return permissionState == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun check(onCompletionCallback: (Boolean) -> Unit) {
        this.onCompletionCallback = onCompletionCallback
        if (!allowed()) {
            request()
        }
    }

    private fun request() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSIONS_READ_STORAGE
            )
        } else {
            throw Exception("Request permissions not required before API 23 (should never happen)")
        }
    }

    fun handleRequestResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS_READ_STORAGE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                if (!shouldProvideRationale) {
                    val dialogBuilder =
                        MaterialAlertDialogBuilder(activity, R.style.BaseAlertDialog).apply {
                            setTitle(null)
                            setMessage(R.string.read_storage_permission_msg)
                            setPositiveButton(R.string.go_to_setting) { _, _ ->
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${activity.packageName}")
                                )
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
        const val REQUEST_PERMISSIONS_READ_STORAGE = 5000
    }
}
