package org.rfcx.audiomoth.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.rfcx.audiomoth.R

class NotificationBroadcastReceiver : BroadcastReceiver() {
    private val channelId = "org.rfcx.audiomoth.view.configure"

    // todo: Change the text displayed for notifications
    override fun onReceive(context: Context, intent: Intent) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.will_run_out_on, " April 11, 2020"))
            .setSmallIcon(R.drawable.ic_audiomoth)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(1, builder.build())
    }
}