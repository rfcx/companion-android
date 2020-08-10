package org.rfcx.audiomoth.view.detail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_edit_location.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.localdb.EdgeDeploymentDb
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.service.DeploymentSyncWorker
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.view.deployment.locate.MapPickerFragment
import java.util.*

class EditLocationActivity : AppCompatActivity(), MapPickerProtocol, EditLocationActivityListener {

    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var nameLocation: String? = null
    private var deploymentId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_location)

        initIntent()
        setupToolbar()
        toolbarLayout.visibility = View.VISIBLE
        startFragment(MapPickerFragment.newInstance(latitude, longitude, nameLocation ?: ""))
    }

    private fun initIntent() {
        intent.extras?.let {
            latitude = it.getDouble(EXTRA_LATITUDE)
            longitude = it.getDouble(EXTRA_LONGITUDE)
            nameLocation = it.getString(EXTRA_LOCATION_NAME)
            deploymentId = it.getInt(EXTRA_DEPLOYMENT_ID)
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

    override fun startDetailDeploymentPage(name: String) {
        val deploymentLocation = DeploymentLocation(
            name = name,
            latitude = latitude,
            longitude = longitude
        )
        deploymentId?.let {
            val edgeDeployment = edgeDeploymentDb.getDeploymentById(it)
            if (edgeDeployment != null) {
                edgeDeployment.updatedAt = Date()
                edgeDeployment.syncState = SyncState.Unsent.key
                edgeDeployment.location = deploymentLocation
                edgeDeploymentDb.updateDeployment(edgeDeployment)

                val edgeServerId = edgeDeployment.serverId
                val location = if (edgeServerId != null) {
                    locateDb.getLocateByServerId(edgeServerId)
                } else {
                    locateDb.getLocateByEdgeDeploymentId(edgeDeployment.id)
                }

                if (location != null) {
                    location.latitude = deploymentLocation.latitude
                    location.longitude = deploymentLocation.longitude
                    location.name = deploymentLocation.name
                    location.syncState = SyncState.Unsent.key
                    locateDb.updateLocate(location)
                }

                DeploymentSyncWorker.enqueue(this)
                finish()
            }
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

    companion object {
        const val EXTRA_LATITUDE = "EXTRA_LATITUDE"
        const val EXTRA_LONGITUDE = "EXTRA_LONGITUDE"
        const val EXTRA_LOCATION_NAME = "EXTRA_LOCATION_NAME"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"

        fun startActivity(
            context: Context,
            lat: Double,
            lng: Double,
            name: String,
            deploymentId: Int,
            requestCode: Int
        ) {
            val intent = Intent(context, EditLocationActivity::class.java)
            intent.putExtra(EXTRA_LATITUDE, lat)
            intent.putExtra(EXTRA_LONGITUDE, lng)
            intent.putExtra(EXTRA_LOCATION_NAME, name)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
