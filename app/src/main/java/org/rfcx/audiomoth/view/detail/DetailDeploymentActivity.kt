package org.rfcx.audiomoth.view.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_detail_deployment.*
import kotlinx.android.synthetic.main.activity_feedback.toolbar
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.toDateTimeString
import org.rfcx.audiomoth.view.deployment.DeploymentActivity.Companion.DEPLOYMENT_ID
import org.rfcx.audiomoth.view.deployment.ImageAdapter
import java.util.*

class DetailDeploymentActivity : AppCompatActivity() {
    var deployment: Deployment? = null
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val gainList = arrayOf("Low", "Low - Medium", "Medium", "Medium - High", "High")
    private val imageAdapter by lazy { ImageAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_deployment)

        val deploymentId = intent.extras?.getInt(DEPLOYMENT_ID)
        if (deploymentId != null) {
            deployment = deploymentDb.getDeploymentById(deploymentId)
            val location = deployment?.location
            val configuration = deployment?.configuration

            locationLongitudeValue.text = location?.longitude.toString()
            locationLatitudeValue.text = location?.latitude.toString()

            sampleRateValue.text =
                getString(R.string.kilohertz, configuration?.sampleRate.toString())
            gainValue.text = configuration?.gain?.let { gainList[it] }
            recordingValue.text = getString(
                if (configuration?.recordingDuration == 1) R.string.detail_sec else R.string.detail_secs,
                configuration?.recordingDuration
            )
            sleepValue.text = getString(R.string.detail_secs, configuration?.sleepDuration)
            estimatedBatteryDurationValue.text =
                deployment?.batteryDepletedAt?.time?.let { Date(it).toDateTimeString() }
        }

        reconfigureButton.setOnClickListener {
            Toast.makeText(this, R.string.reconfigure, Toast.LENGTH_LONG).show()
        }

        setupToolbar()
        setupImageRecycler()
    }

    private fun setupImageRecycler() {
        attachImageRecycler.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            elevation = 0f
            title = if (deployment != null) deployment?.location?.name else "Location name"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, DetailDeploymentActivity::class.java)
            intent.putExtra(DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }
}
