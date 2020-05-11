package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.Stream
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICE_ID

class ConfigureActivity : AppCompatActivity(), ConfigureListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)

        if (intent.hasExtra(DEVICE_ID) && intent.hasExtra(STREAM_NAME) && intent.hasExtra(SITE_ID) && intent.hasExtra(
                SITE_NAME
            ) && intent.hasExtra(FROM)
        ) {
            val deviceId = intent.getStringExtra(DEVICE_ID)
            val streamName = intent.getStringExtra(STREAM_NAME)
            val siteId = intent.getStringExtra(SITE_ID)
            val siteName = intent.getStringExtra(SITE_NAME)
            val from = intent.getStringExtra(FROM)

            if (deviceId != null && streamName != null && from != null && siteId != null && siteName != null) {
                val streamDefault = Stream(
                    streamName,
                    3,
                    8,
                    false,
                    0,
                    0,
                    arrayListOf(),
                    ConfigureFragment.RECOMMENDED
                )

                supportFragmentManager.beginTransaction()
                    .add(
                        configureContainer.id,
                        ConfigureFragment.newInstance(
                            deviceId,
                            siteId,
                            siteName,
                            streamDefault,
                            from
                        ),
                        "ConfigureFragment"
                    ).commit()
            }
        }
    }

    override fun openSync(device: Device) {
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

    override fun openPerformBattery() {
        supportFragmentManager.beginTransaction()
            .replace(
                configureContainer.id, PerformBatteryFragment(),
                PerformBatteryFragment.TAG
            ).commit()
    }

    override fun openDeploy(batteryLv: Int, datePredict: Long) {
        if (intent.hasExtra(DEVICE_ID)) {
            val deviceId = intent.getStringExtra(DEVICE_ID)
            if (deviceId != null) {
                supportFragmentManager.beginTransaction()
                    .replace(
                        configureContainer.id,
                        DeployFragment.newInstance(deviceId, batteryLv, datePredict),
                        DeployFragment.TAG
                    ).commit()
            }
        }
    }

    companion object {
        const val STREAM_NAME = "STREAM_NAME"
        const val SITE_ID = "SITE_ID"
        const val SITE_NAME = "SITE_NAME"
        const val FROM = "FROM"

        fun startActivity(
            context: Context,
            deviceId: String,
            streamName: String,
            siteId: String,
            siteName: String,
            from: String
        ) {
            val intent = Intent(context, ConfigureActivity::class.java)
            intent.putExtra(DEVICE_ID, deviceId)
            intent.putExtra(STREAM_NAME, streamName)
            intent.putExtra(SITE_ID, siteId)
            intent.putExtra(SITE_NAME, siteName)
            intent.putExtra(FROM, from)
            context.startActivity(intent)
        }
    }
}

interface ConfigureListener {
    fun openSync(device: Device)
    fun openVerifySync()
    fun openPerformBattery()
    fun openDeploy(batteryLv: Int, datePredict: Long)
}
