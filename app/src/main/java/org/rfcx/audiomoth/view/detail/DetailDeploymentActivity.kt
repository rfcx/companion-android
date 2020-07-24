package org.rfcx.audiomoth.view.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_detail_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentImage
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.localdb.DeploymentImageDb
import org.rfcx.audiomoth.util.*
import org.rfcx.audiomoth.view.deployment.DeploymentActivity.Companion.DEPLOYMENT_ID
import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment
import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment.Companion.CONTINUOUS
import java.util.*
import kotlin.collections.ArrayList

class DetailDeploymentActivity : AppCompatActivity() {
    var deployment: Deployment? = null
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val gainList = arrayOf("Low", "Low - Medium", "Medium", "Medium - High", "High")
    private val deploymentImageAdapter by lazy { DeploymentImageAdapter() }
    private val timeLineAdapter by lazy { TimeLineAdapter() }

    // data
    private var deploymentImages = listOf<DeploymentImage>()
    private lateinit var deployImageLiveData: LiveData<List<DeploymentImage>>
    private val deploymentImageObserve = Observer<List<DeploymentImage>> {
        deploymentImages = it
        updateDeploymentImages(deploymentImages)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_deployment)

        val deploymentId = intent.extras?.getInt(DEPLOYMENT_ID)
        if (deploymentId != null) {
            // setup deployment images view
            setupImageRecycler()
            observeDeploymentImage(deploymentId)

            deployment = deploymentDb.getDeploymentById(deploymentId)
            val location = deployment?.location
            val configuration = deployment?.configuration

            locationLongitudeValue.text = location?.longitude.longitudeCoordinates(this)
            locationLatitudeValue.text = location?.latitude.latitudeCoordinates(this)

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
        }

        reconfigureButton.setOnClickListener {
            Toast.makeText(this, R.string.reconfigure, Toast.LENGTH_LONG).show()
        }

        setupToolbar()

    }

    private fun observeDeploymentImage(deploymentId: Int) {
        deployImageLiveData =
            Transformations.map(deploymentImageDb.getAllResultsAsync(deploymentId).asLiveData()) {
                it
            }
        deployImageLiveData.observeForever(deploymentImageObserve)
    }

    private fun updateDeploymentImages(deploymentImages: List<DeploymentImage>) {
        photoLabel.visibility = if (deploymentImages.isNotEmpty()) View.VISIBLE else View.GONE
        deploymentImageRecycler.visibility =
            if (deploymentImages.isNotEmpty()) View.VISIBLE else View.GONE
        val items = deploymentImages.map { it.toDeploymentImageView() }
        deploymentImageAdapter.submitList(items)
    }

    private fun setupImageRecycler() {
        deploymentImageRecycler.apply {
            adapter = deploymentImageAdapter
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
            title = if (deployment != null) deployment?.location?.name else "Location name"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // remove observer
        deployImageLiveData.removeObserver(deploymentImageObserve)
    }

    companion object {
        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, DetailDeploymentActivity::class.java)
            intent.putExtra(DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }
}
