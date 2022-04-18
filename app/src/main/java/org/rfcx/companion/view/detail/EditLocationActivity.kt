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
import org.rfcx.companion.entity.LocationGroup
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.toLocationGroup
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
import org.rfcx.companion.view.profile.locationgroup.LocationGroupActivity

class EditLocationActivity : AppCompatActivity(), MapPickerProtocol, EditLocationActivityListener {
    private lateinit var viewModel: EditLocationViewModel

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var nameLocation: String? = null
    private var deploymentId: Int? = null
    private var groupName: String? = null
    private var device: String? = null
    private var locationGroup: LocationGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_location)

        initIntent()
        setViewModel()
        setupToolbar()
        toolbarLayout.visibility = View.VISIBLE
        startFragment(
            MapPickerFragment.newInstance(
                latitude,
                longitude,
                altitude,
                nameLocation ?: ""
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
                LocationGroupActivity.RESULT_OK -> {
                    locationGroup =
                        data?.getSerializableExtra(EXTRA_LOCATION_GROUP) as LocationGroup
                    locationGroup?.let {
                        val isGroupExisted = viewModel.isExisted(locationGroup?.name)
                        groupName = if (isGroupExisted) {
                            it.name
                        } else {
                            getString(R.string.none)
                        }
                    }
                }
                LocationGroupActivity.RESULT_DELETE -> {
                    val group = data?.getStringExtra(LocationGroupActivity.EXTRA_GROUP)
                    val isGroupExisted = viewModel.isExisted(group)
                    groupName = if (isGroupExisted) {
                        group
                    } else {
                        getString(R.string.none)
                    }
                }
            }
        }
    }

    private fun initIntent() {
        intent.extras?.let {
            latitude = it.getDouble(EXTRA_LATITUDE)
            longitude = it.getDouble(EXTRA_LONGITUDE)
            altitude = it.getDouble(ARG_ALTITUDE)
            nameLocation = it.getString(EXTRA_LOCATION_NAME)
            deploymentId = it.getInt(EXTRA_DEPLOYMENT_ID)
            groupName = it.getString(EXTRA_LOCATION_GROUP_NAME)
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
        startFragment(EditLocationFragment.newInstance(latitude, longitude, altitude, name))
    }

    override fun startMapPickerPage(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        name: String
    ) {
        toolbarLayout.visibility = View.VISIBLE
        setLatLng(latitude, longitude, altitude)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, altitude, name))
    }

    override fun updateDeploymentDetail(name: String, altitude: Double) {
        val group = groupName ?: ""
        deploymentId?.let { id ->
            viewModel.editStream(
                id = id,
                locationName = name,
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                callback = object : DatabaseCallback {
                    override fun onSuccess() {
                        DeploymentSyncWorker.enqueue(this@EditLocationActivity)
                        finish()
                    }

                    override fun onFailure(errorMessage: String) {
                        showCommonDialog(errorMessage)
                    }
                }
            )

            viewModel.editProject(
                id, getLocationGroup(group),
                object :
                    DatabaseCallback {
                    override fun onSuccess() {
                        DeploymentSyncWorker.enqueue(this@EditLocationActivity)
                        finish()
                    }

                    override fun onFailure(errorMessage: String) {
                        showCommonDialog(errorMessage)
                    }
                }
            )
        }
    }

    override fun getLocationGroupName(): String = groupName ?: getString(R.string.none)

    override fun getLocationGroup(name: String): LocationGroup {
        return viewModel.getProjectByName(name)?.toLocationGroup() ?: LocationGroup()
    }

    override fun startLocationGroupPage() {
        val setLocationGroup = if (groupName == getString(R.string.none)) null else groupName
        intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)?.let { deploymentId ->
            LocationGroupActivity.startActivity(
                this,
                setLocationGroup,
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
        const val EXTRA_LATITUDE = "EXTRA_LATITUDE"
        const val EXTRA_LONGITUDE = "EXTRA_LONGITUDE"
        const val ARG_ALTITUDE = "ARG_ALTITUDE"
        const val EXTRA_LOCATION_NAME = "EXTRA_LOCATION_NAME"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        const val EXTRA_LOCATION_GROUP_NAME = "EXTRA_LOCATION_GROUP_NAME"
        const val EXTRA_LOCATION_GROUP = "EXTRA_LOCATION_GROUP"
        const val EXTRA_DEVICE = "EXTRA_DEVICE"

        fun startActivity(
            context: Context,
            lat: Double,
            lng: Double,
            altitude: Double,
            name: String,
            deploymentId: Int,
            groupName: String,
            device: String,
            requestCode: Int
        ) {
            val intent = Intent(context, EditLocationActivity::class.java)
            intent.putExtra(EXTRA_LATITUDE, lat)
            intent.putExtra(EXTRA_LONGITUDE, lng)
            intent.putExtra(ARG_ALTITUDE, altitude)
            intent.putExtra(EXTRA_LOCATION_NAME, name)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            intent.putExtra(EXTRA_LOCATION_GROUP_NAME, groupName)
            intent.putExtra(EXTRA_DEVICE, device)
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
