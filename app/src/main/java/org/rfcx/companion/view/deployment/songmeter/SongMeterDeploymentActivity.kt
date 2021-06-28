package org.rfcx.companion.view.deployment.songmeter

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.view.deployment.EdgeDeploymentActivity

class SongMeterDeploymentActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SongMeterDeploymentActivity"

        fun startActivity(context: Context) {
            val intent = Intent(context, SongMeterDeploymentActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_meter_deployment)

        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }
}
