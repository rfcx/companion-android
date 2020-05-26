package org.rfcx.audiomoth.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.configure.ConfigureFragment.Companion.CHANNEL_ID
import org.rfcx.audiomoth.view.configure.PerformBatteryFragment
import org.rfcx.audiomoth.view.configure.PerformBatteryFragment.Companion.BATTERY_DEPLETED_AT

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val batteryDepletedAt = intent.extras?.getString(BATTERY_DEPLETED_AT)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.will_run_out_on, batteryDepletedAt))
            .setSmallIcon(R.drawable.ic_audiomoth)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(1, builder.build())
    }
}