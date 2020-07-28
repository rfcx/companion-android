package org.rfcx.audiomoth.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import org.rfcx.audiomoth.R

class LocationPermissions(private val activity: Activity) {
    private val locationRequest = LocationRequest().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private var onCompletionCallback: ((Boolean) -> Unit)? = null

    private fun allowed(): Boolean {
        val permissionState =
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    fun check(onCompletionCallback: (Boolean) -> Unit) {
        this.onCompletionCallback = onCompletionCallback
        if (!allowed()) {
            request()
        } else {
            verifySettings()
        }
    }

    private fun request() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        } else {
            throw Exception("Request permissions not required before API 23 (should never happen)")
        }
    }

    fun handleRequestResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (!shouldProvideRationale) {
                    val dialogBuilder: AlertDialog.Builder =
                        AlertDialog.Builder(activity).apply {
                            setTitle(null)
                            setMessage(R.string.location_permission_msg)
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
                verifySettings()
            }
        }
    }

    private fun verifySettings() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            onCompletionCallback?.invoke(true)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog
                try {
                    // Show the dialog and check the result in onActivityResult()
                    exception.startResolutionForResult(activity, REQUEST_CHECK_LOCATION_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                    sendEx.printStackTrace()
                    onCompletionCallback?.invoke(false)
                }
            } else {
                onCompletionCallback?.invoke(false)
            }
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS) {
            onCompletionCallback?.invoke(resultCode == Activity.RESULT_OK)
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_CHECK_LOCATION_SETTINGS = 35
    }
}
