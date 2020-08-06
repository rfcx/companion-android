package org.rfcx.audiomoth.util

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.rfcx.audiomoth.MainActivity
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment.Companion.CHANNEL_ID
import org.rfcx.audiomoth.view.deployment.verify.PerformBatteryFragment
import org.rfcx.audiomoth.view.deployment.verify.PerformBatteryFragment.Companion.BATTERY_DEPLETED_AT
import org.rfcx.audiomoth.view.deployment.verify.PerformBatteryFragment.Companion.LOCATION_NAME

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val batteryDepletedAt = intent.extras?.getString(BATTERY_DEPLETED_AT)
        val locationName = intent.extras?.getString(LOCATION_NAME)
        val edgeDeploymentId = intent.extras?.getString(PerformBatteryFragment.EXTRA_EDGE_DEPLOYMENT_ID)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val intentData = Intent(context, MainActivity::class.java)
        intentData.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intentData.putExtra(PerformBatteryFragment.EXTRA_EDGE_DEPLOYMENT_ID, edgeDeploymentId)

        val pendingIntent = PendingIntent.getActivity(context, 0, intentData, PendingIntent.FLAG_ONE_SHOT)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setAutoCancel(true)
            .setContentTitle(context.getString(R.string.edge_device_battery))
            .setContentIntent(pendingIntent)
            .setContentText(
                context.getString(
                    R.string.will_run_out_on,
                    locationName,
                    batteryDepletedAt
                )
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setSound(defaultSoundUri)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(1, builder.build())
    }
}
