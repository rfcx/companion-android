package org.rfcx.companion.view.detail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_edit_location.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.showCommonDialog
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.detail.DeploymentDetailActivity.Companion.DEPLOYMENT_REQUEST_CODE
import org.rfcx.companion.view.profile.locationgroup.ProjectActivity

class EditLocationActivity : AppCompatActivity(), MapPickerProtocol, EditLocationActivityListener {
    private lateinit var viewModel: EditLocationViewModel

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var streamId: Int? = null
    private var deploymentId: Int? = null
    private var selectedProject: Int? = null
    private var device: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_location)

        setViewModel()
        setupToolbar()
        initIntent()
        toolbarLayout.visibility = View.VISIBLE

        startFragment(
            MapPickerFragment.newInstance(
                latitude,
                longitude,
                altitude,
                streamId ?: -1
            )
        )
    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                CoreApiHelper(CoreApiServiceImpl()),
                LocalDataHelper()
            )
        ).get(EditLocationViewModel::class.java)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEPLOYMENT_REQUEST_CODE) {
            when (resultCode) {
                ProjectActivity.RESULT_OK -> {
                    selectedProject =
                        data?.getIntExtra(EXTRA_PROJECT_ID, -1)
                }
                ProjectActivity.RESULT_DELETE -> {
                    selectedProject = data?.getIntExtra(EXTRA_PROJECT_ID, -1)
                }
            }
        }
    }

    private fun initIntent() {
        intent.extras?.let {
            streamId = it.getInt(EXTRA_STREAM_ID)
            val stream = viewModel.getStreamById(streamId ?: -1)
            latitude = stream?.latitude ?: 0.0
            longitude = stream?.longitude ?: 0.0
            altitude = stream?.altitude ?: 0.0

            deploymentId = it.getInt(EXTRA_DEPLOYMENT_ID)
            selectedProject = it.getInt(EXTRA_PROJECT_ID)
            device = it.getString(EXTRA_DEVICE)
        }
    }

    private fun setLatLng(latitude: Double, longitude: Double, altitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
        this.altitude = altitude
    }

    override fun showAppbar() {
        toolbarLayout.visibility = View.VISIBLE
    }

    override fun hideAppbar() {
        toolbarLayout.visibility = View.GONE
    }

    override fun onSelectedLocation(
        latitude: Double,
        longitude: Double,
        siteId: Int,
        name: String
    ) {
        toolbarLayout.visibility = View.VISIBLE
        setLatLng(latitude, longitude, altitude)
        startFragment(EditLocationFragment.newInstance(latitude, longitude, altitude, siteId))
    }

    override fun startMapPickerPage(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        streamId: Int
    ) {
        toolbarLayout.visibility = View.VISIBLE
        setLatLng(latitude, longitude, altitude)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, altitude, streamId))
    }

    override fun updateDeploymentDetail(name: String, altitude: Double) {
        val stream = viewModel.getStreamById(streamId ?: -1)
        stream?.let { it ->
            viewModel.editStream(
                id = it.id,
                locationName = name,
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                selectedProject ?: -1
            )
        }
        viewModel.markDeploymentNeedUpdate(deploymentId ?: -1)
        DeploymentSyncWorker.enqueue(this@EditLocationActivity)
        finish()
    }

    override fun getStream(id: Int): Stream {
        return viewModel.getStreamById(id) ?: Stream()
    }

    override fun getProject(id: Int): Project {
        return viewModel.getProjectById(id) ?: Project()
    }

    override fun startLocationGroupPage() {
        intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)?.let { deploymentId ->
            ProjectActivity.startActivity(
                this,
                selectedProject ?: -1,
                deploymentId,
                Screen.EDIT_LOCATION.id,
                DEPLOYMENT_REQUEST_CODE
            )
        }
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(editLocationContainer.id, fragment)
            .commit()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.edit_location)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    companion object {
        const val EXTRA_STREAM_ID = "EXTRA_STREAM_ID"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        const val EXTRA_PROJECT_ID = "EXTRA_PROJECT_ID"
        const val EXTRA_DEVICE = "EXTRA_DEVICE"

        fun startActivity(
            context: Context,
            streamId: Int,
            deploymentId: Int,
            projectId: Int,
            device: String,
            requestCode: Int
        ) {
            val intent = Intent(context, EditLocationActivity::class.java)
            intent.putExtra(EXTRA_STREAM_ID, streamId)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            intent.putExtra(EXTRA_PROJECT_ID, projectId)
            intent.putExtra(EXTRA_DEVICE, device)
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
