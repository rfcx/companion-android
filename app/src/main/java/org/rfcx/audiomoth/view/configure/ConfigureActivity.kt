package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_configure.*
import org.rfcx.audiomoth.R

class ConfigureActivity : AppCompatActivity(), ConfigureListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)

        supportFragmentManager.beginTransaction()
            .add(configureContainer.id, LocationFragment(), LocationFragment.TAG).commit()
    }

    override fun openSync() {
        supportFragmentManager.beginTransaction()
            .replace(configureContainer.id, SyncFragment(), "SyncFragment").commit()
    }

    override fun openVerifySync() {
        supportFragmentManager.beginTransaction()
            .replace(configureContainer.id, VerifySyncFragment(), "VerifySyncFragment").commit()
    }

    override fun openPerformBattery() {
        supportFragmentManager.beginTransaction()
            .replace(configureContainer.id, PerformBatteryFragment(), PerformBatteryFragment.TAG)
            .commit()
    }

    override fun openDeploy(batteryLv: Int, datePredict: Long) {
        supportFragmentManager.beginTransaction()
            .replace(configureContainer.id, LocationFragment(), LocationFragment.TAG).commit()
    }

    companion object {
        const val SITE_ID = "SITE_ID"
        const val SITE_NAME = "SITE_NAME"
        const val FROM = "FROM"

        fun startActivity(context: Context) {
            val intent = Intent(context, ConfigureActivity::class.java)
            context.startActivity(intent)
        }
    }
}

interface ConfigureListener {
    fun openSync()
    fun openVerifySync()
    fun openPerformBattery()
    fun openDeploy(batteryLv: Int, datePredict: Long)
}
