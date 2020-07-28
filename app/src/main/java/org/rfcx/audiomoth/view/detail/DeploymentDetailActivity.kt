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
import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.activity_deployment_detail.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentImage
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.localdb.DeploymentImageDb
import org.rfcx.audiomoth.util.*
import org.rfcx.audiomoth.util.Battery.getEstimatedBatteryDuration
import org.rfcx.audiomoth.view.deployment.DeploymentActivity.Companion.EXTRA_DEPLOYMENT_ID
import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment.Companion.CONTINUOUS

class DeploymentDetailActivity : AppCompatActivity() {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val gainList by lazy { resources.getStringArray(R.array.edge_gains) }
    private val deploymentImageAdapter by lazy { DeploymentImageAdapter() }
    private val timeLineAdapter by lazy { TimeLineAdapter() }

    // data
    private var deployment: Deployment? = null
    private lateinit var deployImageLiveData: LiveData<List<DeploymentImage>>
    private var deploymentImages = listOf<DeploymentImage>()
    private val deploymentImageObserve = Observer<List<DeploymentImage>> {
        deploymentImages = it
        updateDeploymentImages(deploymentImages)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment_detail)

        deployment =
            intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)?.let { deploymentDb.getDeploymentById(it) }

        setupToolbar()
        updateDeploymentDetailView()

        // setup onclick
        reconfigureButton.setOnClickListener {
            Toast.makeText(this, R.string.reconfigure, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateDeploymentDetailView() {
        deployment?.let {
            // setup deployment images view
            setupImageRecycler()
            observeDeploymentImage(it.id)

            val location = deployment?.location
            val configuration = deployment?.configuration

            locationLongitudeValue.text = location?.longitude.longitudeCoordinates(this)
            locationLatitudeValue.text = location?.latitude.latitudeCoordinates(this)

            sampleRateValue.text =
                getString(R.string.kilohertz, configuration?.sampleRate.toString())
            gainValue.text = configuration?.gain?.let { gain -> gainList[gain] }

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
                deployment?.batteryDepletedAt?.time?.let { depletedAt ->
                    getEstimatedBatteryDuration(this, depletedAt)
                }
            configuration?.recordingPeriodList?.let { period ->
                customRecordingLabel.visibility = if (period.size != 0) View.VISIBLE else View.GONE
                timeLineRecycler.visibility = if (period.size != 0) View.VISIBLE else View.GONE
                setupTimeLineRecycler(period.toTypedArray())
            }
        }
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
        val timeList = EdgeConfigure.configureTimes
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
            title = deployment?.location?.name ?: getString(R.string.title_deployment_detail)
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
            val intent = Intent(context, DeploymentDetailActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }
}
