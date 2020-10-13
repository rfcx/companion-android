package org.rfcx.audiomoth.view.detail

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_deployment_detail.*
import kotlinx.android.synthetic.main.activity_deployment_detail.locationValueTextView
import kotlinx.android.synthetic.main.activity_deployment_detail.pinDeploymentImageView
import kotlinx.android.synthetic.main.fragment_edit_location.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentImage
import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.Screen
import org.rfcx.audiomoth.entity.toLocationGroup
import org.rfcx.audiomoth.localdb.DatabaseCallback
import org.rfcx.audiomoth.localdb.DeploymentImageDb
import org.rfcx.audiomoth.localdb.EdgeDeploymentDb
import org.rfcx.audiomoth.localdb.LocationGroupDb
import org.rfcx.audiomoth.service.DeploymentSyncWorker
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.asLiveData
import org.rfcx.audiomoth.util.convertLatLngLabel
import org.rfcx.audiomoth.util.showCommonDialog
import org.rfcx.audiomoth.view.BaseActivity
import org.rfcx.audiomoth.view.deployment.EdgeDeploymentActivity.Companion.EXTRA_DEPLOYMENT_ID
import org.rfcx.audiomoth.view.profile.locationgroup.LocationGroupActivity
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment

class DeploymentDetailActivity : BaseActivity(), OnMapReadyCallback {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val deploymentImageAdapter by lazy { DeploymentImageAdapter() }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }

    private lateinit var mapView: MapView
    private lateinit var mapBoxMap: MapboxMap

    // data
    private var deployment: EdgeDeployment? = null
    private lateinit var deployImageLiveData: LiveData<List<DeploymentImage>>
    private var deploymentImages = listOf<DeploymentImage>()
    private val deploymentImageObserve = Observer<List<DeploymentImage>> {
        deploymentImages = it
        updateDeploymentImages(deploymentImages)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_token))
        setContentView(R.layout.activity_deployment_detail)

        // Setup Mapbox
        mapView = findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        deployment =
            intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
                ?.let { edgeDeploymentDb.getDeploymentById(it) }

        setupToolbar()
        setupImageRecycler()
        deployment?.let { updateDeploymentDetailView(it) }

        // setup onclick
        deleteButton.setOnClickListener {
            confirmationDialog()
        }

        editButton.setOnClickListener {
            deployment?.let {
                val location = deployment?.location
                location?.let { locate ->
                    intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)?.let { deploymentId ->
                        EditLocationActivity.startActivity(
                            this,
                            locate.latitude,
                            locate.longitude,
                            locate.name,
                            deploymentId,
                            locationGroupValueTextView.text.toString(),
                            DEPLOYMENT_REQUEST_CODE
                        )
                    }
                }
            }
        }

        editGroupButton.setOnClickListener {
            val group = locationGroupValueTextView.text.toString()
            val setLocationGroup = if (group == getString(R.string.none)) null else group
            intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)?.let { deploymentId ->
                LocationGroupActivity.startActivity(
                    this,
                    setLocationGroup,
                    deploymentId,
                    Screen.EDGE_DETAIL.id,
                    DEPLOYMENT_REQUEST_CODE
                )
            }
        }
    }

    private fun confirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.delete_location))
        builder.setMessage(getString(R.string.are_you_sure_delete_location))

        builder.setPositiveButton(getString(R.string.delete)) { _, _ ->
            onDeleteLocation()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { _, _ -> }

        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
    }

    private fun onDeleteLocation() {
        showLoading()
        deployment?.let {
            edgeDeploymentDb.deleteDeploymentLocation(it.id, object : DatabaseCallback {
                override fun onSuccess() {
                    DeploymentSyncWorker.enqueue(this@DeploymentDetailActivity)
                    hideLoading()
                    finish()
                }

                override fun onFailure(errorMessage: String) {
                    hideLoading()
                    showCommonDialog(errorMessage)
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEPLOYMENT_REQUEST_CODE) {
            forceUpdateDeployment()
        }
    }

    private fun forceUpdateDeployment() {
        if (this.deployment != null) {
            this.deployment = edgeDeploymentDb.getDeploymentById(this.deployment!!.id)
            this.deployment?.let { it1 ->
                updateDeploymentDetailView(it1)
                setLocationOnMap(it1)
            }

            supportActionBar?.apply {
                title = deployment?.location?.name ?: getString(R.string.title_deployment_detail)
            }
        }
    }

    private fun updateDeploymentDetailView(deployment: EdgeDeployment) {
        // setup deployment images view
        observeDeploymentImage(deployment.id)

        val location = deployment.location
        locationValueTextView.text =
            location?.let { locate ->
                convertLatLngLabel(this, locate.latitude, locate.longitude)
            }

        locationGroupValueTextView.text =
            location?.locationGroup?.let { locationGroup ->
                if (locationGroup.group.isNullOrBlank()) {
                    getString(R.string.none)
                } else {
                    locationGroup.group
                }
            } ?: getString(R.string.none)

        changePinColorByGroup(location?.locationGroup?.group ?: getString(R.string.none))
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

    private fun changePinColorByGroup(group: String) {
        val locationGroup = locationGroupDb.getLocationGroup(group).toLocationGroup()
        val color = locationGroup.color
        val pinDrawable = pinDeploymentImageView.drawable
        if (color != null && color.isNotEmpty() && group != getString(R.string.none)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                pinDrawable.setColorFilter(color.toColorInt(), PorterDuff.Mode.SRC_ATOP)
            } else {
                pinDrawable.setTint(color.toColorInt())
            }
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                pinDrawable.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorPrimary
                    ), PorterDuff.Mode.SRC_ATOP
                )
            } else {
                pinDrawable.setTint(ContextCompat.getColor(this, R.color.colorPrimary))
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapBoxMap = mapboxMap
        mapboxMap.uiSettings.apply {
            setAllGesturesEnabled(false)
            isAttributionEnabled = false
            isLogoEnabled = false
        }

        mapboxMap.setStyle(Style.OUTDOORS) {
            deployment?.let { it1 -> setLocationOnMap(it1) }
        }
    }

    private fun setLocationOnMap(deployment: EdgeDeployment) {
        val location = deployment.location
        location?.let { locate ->
            val latLng = LatLng(locate.latitude, locate.longitude)
            moveCamera(latLng, LocationFragment.DEFAULT_ZOOM)
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Double) {
        mapBoxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
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

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        const val DEPLOYMENT_REQUEST_CODE = 1001

        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, DeploymentDetailActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }
}
