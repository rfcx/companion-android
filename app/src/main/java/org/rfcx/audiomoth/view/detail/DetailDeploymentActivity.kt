package org.rfcx.audiomoth.view.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_detail_deployment.*
import kotlinx.android.synthetic.main.activity_feedback.toolbar
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.repo.Firestore
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.convertToStopStartPeriods
import org.rfcx.audiomoth.util.toDateTimeString
import org.rfcx.audiomoth.view.deployment.DeploymentActivity.Companion.DEPLOYMENT_ID
import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment
import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment.Companion.CONTINUOUS
import java.util.*
import kotlin.collections.ArrayList

class DetailDeploymentActivity : AppCompatActivity() {
    var deployment: Deployment? = null
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val gainList = arrayOf("Low", "Low - Medium", "Medium", "Medium - High", "High")
    private val imageAdapter by lazy { ImageDetailAdapter() }
    private val timeLineAdapter by lazy { TimeLineAdapter() }

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

            val continuous = getString(R.string.continuous)
            val isContinuous = configuration?.durationSelected == CONTINUOUS

            val recordingDurationLabel = getString(
                if (configuration?.recordingDuration == 1) R.string.detail_sec else R.string.detail_secs,
                configuration?.recordingDuration
            )
            recordingValue.text = if (isContinuous) continuous else recordingDurationLabel
            sleepValue.text = getString(R.string.detail_secs, configuration?.sleepDuration)
            sleepValue.visibility = if (isContinuous) View.GONE else View.VISIBLE
            sleepLabel.visibility = if (isContinuous) View.GONE else View.VISIBLE

            estimatedBatteryDurationValue.text =
                deployment?.batteryDepletedAt?.time?.let { Date(it).toDateTimeString() }
            configuration?.recordingPeriodList?.let {
                customRecordingLabel.visibility = if (it.size != 0) View.VISIBLE else View.GONE
                timeLineRecycler.visibility = if (it.size != 0) View.VISIBLE else View.GONE
                setupTimeLineRecycler(it.toTypedArray())
            }
            deployment?.serverId?.let {
                setupImageRecycler()
                Firestore(this).getRemotePathByServerId(it) { remotePathList ->
                    if (remotePathList != null) {
                        photoLabel.visibility = if(remotePathList.size > 0) View.VISIBLE else View.GONE
                        attachImageRecycler.visibility = if(remotePathList.size > 0) View.VISIBLE else View.GONE
                        imageAdapter.items = remotePathList
                    }
                }
            }
        }

        reconfigureButton.setOnClickListener {
            Toast.makeText(this, R.string.reconfigure, Toast.LENGTH_LONG).show()
        }

        setupToolbar()

    }

    private fun setupImageRecycler() {
        attachImageRecycler.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
    }

    private fun setupTimeLineRecycler(selectTimeList: Array<String>) {
        val timeList = ConfigureFragment().timeList
        val array = ArrayList<Boolean>()

        timeLineRecycler.apply {
            adapter = timeLineAdapter
            layoutManager = LinearLayoutManager(context)
        }

        timeList.forEach {
            array.add(selectTimeList.contains(it))
        }

        val recordingPeriod = convertToStopStartPeriods(array.toTypedArray())
        val arrayRecordingPeriod = arrayListOf<String>()
        recordingPeriod?.forEach {
            arrayRecordingPeriod.add("${timeList[it.startMinutes / 60]} - ${if (it.stopMinutes / 60 == 24) timeList[0] else timeList[it.stopMinutes / 60]}")
        }
        timeLineAdapter.items = arrayRecordingPeriod
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
