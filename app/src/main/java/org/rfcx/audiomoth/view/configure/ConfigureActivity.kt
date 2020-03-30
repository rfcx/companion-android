package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Stream
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICE_ID

class ConfigureActivity : AppCompatActivity(), ConfigureListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)

        if (intent.hasExtra(DEVICE_ID) && intent.hasExtra(STREAM_NAME) && intent.hasExtra(STREAM)) {
            val deviceId = intent.getStringExtra(DEVICE_ID)
            val streamName = intent.getStringExtra(STREAM_NAME)
            val stream = intent.getSerializableExtra(STREAM) as? Stream
            if (deviceId != null && streamName != null && stream !== null) {
                supportFragmentManager.beginTransaction()
                    .add(
                        configureContainer.id,
                        ConfigureFragment.newInstance(deviceId, streamName, stream),
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
        if (intent.hasExtra(DEVICE_ID)) {
            val deviceId = intent.getStringExtra(DEVICE_ID)
            if (deviceId != null) {
                supportFragmentManager.beginTransaction()
                    .replace(
                        configureContainer.id, VerifySyncFragment.newInstance(deviceId),
                        "VerifySyncFragment"
                    ).commit()
            }
        }
    }

    companion object {
        const val STREAM_NAME = "STREAM_NAME"
        const val STREAM = "STREAM"

        fun startActivity(context: Context, deviceId: String, streamName: String, stream: Stream) {
            val intent = Intent(context, ConfigureActivity::class.java)
            intent.putExtra(DEVICE_ID, deviceId)
            intent.putExtra(STREAM_NAME, streamName)
            intent.putExtra(STREAM, stream)
            context.startActivity(intent)
        }
    }
}

interface ConfigureListener {
    fun openSync()
    fun openVerifySync()
}
