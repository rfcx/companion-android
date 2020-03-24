package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICE_ID

class ConfigureActivity : AppCompatActivity(), ConfigureListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)

        if (intent.hasExtra(DEVICE_ID) && intent.hasExtra(STREAM_NAME)) {
            val deviceId = intent.getStringExtra(DEVICE_ID)
            val streamName = intent.getStringExtra(STREAM_NAME)
            if (deviceId != null && streamName != null) {
                supportFragmentManager.beginTransaction()
                    .add(
                        configureContainer.id, ConfigureFragment.newInstance(deviceId, streamName),
                        "ConfigureFragment"
                    ).commit()
            }
        }
    }

    override fun openSync() {
        supportFragmentManager.beginTransaction()
            .replace(
                configureContainer.id, SyncFragment(),
                "SyncFragment"
            ).commit()
    }

    override fun openVerifySync() {
        supportFragmentManager.beginTransaction()
            .replace(
                configureContainer.id, VerifySyncFragment(),
                "VerifySyncFragment"
            ).commit()
    }

    companion object {
        const val STREAM_NAME = "STREAM_NAME"

        fun startActivity(context: Context, deviceId: String, streamName: String) {
            val intent = Intent(context, ConfigureActivity::class.java)
            intent.putExtra(DEVICE_ID, deviceId)
            intent.putExtra(STREAM_NAME, streamName)
            context.startActivity(intent)
        }
    }
}

interface ConfigureListener {
    fun openSync()
    fun openVerifySync()
}
