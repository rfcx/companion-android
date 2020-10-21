package org.rfcx.companion.view.detail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_edit_location.*
import kotlinx.android.synthetic.main.fragment_edit_location.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.LocationGroup
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.toLocationGroup
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.showCommonDialog
import org.rfcx.companion.view.BaseActivity
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.detail.DeploymentDetailActivity.Companion.DEPLOYMENT_REQUEST_CODE
import org.rfcx.companion.view.profile.locationgroup.LocationGroupActivity

class EditLocationActivity : BaseActivity(), MapPickerProtocol, EditLocationActivityListener {

    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var nameLocation: String? = null
    private var deploymentId: Int? = null
    private var groupName: String? = null
    private var locationGroup: LocationGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_location)

        initIntent()
        setupToolbar()
        toolbarLayout.visibility = View.VISIBLE
        startFragment(MapPickerFragment.newInstance(latitude, longitude, nameLocation ?: ""))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEPLOYMENT_REQUEST_CODE) {
            if (resultCode == LocationGroupActivity.RESULT_OK) {
                locationGroup = data?.getSerializableExtra(EXTRA_LOCATION_GROUP) as LocationGroup
                locationGroup?.let {
                    groupName = it.group
                }
            }
        }
    }

    private fun initIntent() {
        intent.extras?.let {
            latitude = it.getDouble(EXTRA_LATITUDE)
            longitude = it.getDouble(EXTRA_LONGITUDE)
            nameLocation = it.getString(EXTRA_LOCATION_NAME)
            deploymentId = it.getInt(EXTRA_DEPLOYMENT_ID)
            groupName = it.getString(EXTRA_LOCATION_GROUP_NAME)
        }
    }

    private fun setLatLng(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }

    override fun showAppbar() {
        toolbarLayout.visibility = View.VISIBLE
    }

    override fun hideAppbar() {
        toolbarLayout.visibility = View.GONE
    }

    override fun startLocationPage(latitude: Double, longitude: Double, name: String) {
        toolbarLayout.visibility = View.VISIBLE
        setLatLng(latitude, longitude)
        startFragment(EditLocationFragment.newInstance(latitude, longitude, name))
    }

    override fun startMapPickerPage(latitude: Double, longitude: Double, name: String) {
        toolbarLayout.visibility = View.VISIBLE
        setLatLng(latitude, longitude)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, name))
    }

    override fun updateDeploymentDetail(name: String) {
        showLoading()
        Log.d("location", locationGroup?.group ?: "none")
        deploymentId?.let { id ->
            edgeDeploymentDb.editLocation(
                id = id,
                locationName = name,
                latitude = latitude,
                longitude = longitude,
                callback = object : DatabaseCallback {
                    override fun onSuccess() {
                        DeploymentSyncWorker.enqueue(this@EditLocationActivity)
                        finish()
                    }

                    override fun onFailure(errorMessage: String) {
                        hideLoading()
                        showCommonDialog(errorMessage)
                    }
                })

            locationGroup?.let { group ->
                edgeDeploymentDb.editLocationGroup(id, group, object :
                    DatabaseCallback {
                    override fun onSuccess() {
                        hideLoading()
                        DeploymentSyncWorker.enqueue(this@EditLocationActivity)
                        finish()
                    }

                    override fun onFailure(errorMessage: String) {
                        hideLoading()
                        showCommonDialog(errorMessage)
                    }
                })
            }
        }
    }

    override fun getLocationGroupName(): String = groupName ?: getString(R.string.none)

    override fun getLocationGroup(name: String): LocationGroup {
        return locationGroupDb.getLocationGroup(name).toLocationGroup()
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
        const val EXTRA_LOCATION_NAME = "EXTRA_LOCATION_NAME"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        const val EXTRA_LOCATION_GROUP_NAME = "EXTRA_LOCATION_GROUP_NAME"
        const val EXTRA_LOCATION_GROUP = "EXTRA_LOCATION_GROUP"

        fun startActivity(
            context: Context,
            lat: Double,
            lng: Double,
            name: String,
            deploymentId: Int,
            groupName: String,
            requestCode: Int
        ) {
            val intent = Intent(context, EditLocationActivity::class.java)
            intent.putExtra(EXTRA_LATITUDE, lat)
            intent.putExtra(EXTRA_LONGITUDE, lng)
            intent.putExtra(EXTRA_LOCATION_NAME, name)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            intent.putExtra(EXTRA_LOCATION_GROUP_NAME, groupName)
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
