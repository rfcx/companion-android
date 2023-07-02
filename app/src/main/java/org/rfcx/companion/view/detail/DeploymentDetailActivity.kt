package org.rfcx.companion.view.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
//import com.mapbox.mapboxsdk.Mapbox
//import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
//import com.mapbox.mapboxsdk.geometry.LatLng
//import com.mapbox.mapboxsdk.maps.MapView
//import com.mapbox.mapboxsdk.maps.MapboxMap
//import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
//import com.mapbox.mapboxsdk.maps.Style
//import com.mapbox.pluginscalebar.ScaleBarOptions
//import com.mapbox.pluginscalebar.ScaleBarPlugin
import kotlinx.android.synthetic.main.activity_deployment_detail.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.BuildConfig
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.service.DownloadImagesWorker
import org.rfcx.companion.service.images.ImageSyncWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.AudioMothDeploymentActivity.Companion.EXTRA_DEPLOYMENT_ID
import org.rfcx.companion.view.deployment.Image
import org.rfcx.companion.view.detail.image.AddImageActivity
import java.io.File

class DeploymentDetailActivity :
    AppCompatActivity(), OnMapReadyCallback {
    private val deploymentImageAdapter by lazy { DeploymentImageAdapter() }
    private lateinit var viewModel: DeploymentDetailViewModel

    private var map: GoogleMap? = null

    private val analytics by lazy { Analytics(this) }
    private val firebaseCrashlytics by lazy { Crashlytics() }

    // data
    private var deployment: Deployment? = null
    private lateinit var deployImageLiveData: LiveData<List<DeploymentImage>>
    private var deploymentImages = listOf<DeploymentImage>()
    private val deploymentImageObserve = Observer<List<DeploymentImage>> {
        deploymentImages = it
        updateDeploymentImages(deploymentImages)
    }

    private var newImages: List<Image>? = null

    private var imageFile: File? = null
    private val cameraPermissions by lazy { CameraPermissions(this) }
    private val galleryPermissions by lazy { GalleryPermissions(this) }

    private var toAddImage = false

    private lateinit var deploymentLiveData: LiveData<List<Deployment>>
    private val deploymentObserve = Observer<List<Deployment>> {
        deployment?.let {
            val data = viewModel.getDeploymentById(it.id)
            if (data != null) {
                deployment = data
                updateDeploymentDetailView(data)
                setupToolbar()
            }
        }
        moveCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment_detail)
        setViewModel()

        deploymentLiveData = Transformations.map(
            viewModel.getAllDeploymentLocateResultsAsync().asLiveData()
        ) { it }
        deploymentLiveData.observeForever(deploymentObserve)

        val preferences = Preferences.getInstance(this)
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = viewModel.getProjectById(projectId)

        deleteButton.visibility =
            if (project?.permissions == Permissions.ADMIN.value) View.VISIBLE else View.GONE

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        deployment =
            intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
                ?.let { viewModel.getDeploymentById(it) }
        newImages = intent.getSerializableExtra(NEW_IMAGES_EXTRA)?.let {
            it as List<Image>
        }

        setupToolbar()
        setupImageRecycler()
        deployment?.let {
            updateDeploymentDetailView(it)
            downloadPhotos(it.serverId)
        }
        setupClickListener()

        // setup onclick
        deleteButton.setOnClickListener {
            confirmationDialog()
        }

        editButton.setOnClickListener {
            deployment?.let {
                val stream = deployment?.stream
                stream?.let { st ->
                    intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)?.let { deploymentId ->
                        analytics.trackEditLocationEvent()
                        firebaseCrashlytics.setCustomKey(
                            CrashlyticsKey.EditLocation.key,
                            st.serverId ?: ""
                        )
                        EditLocationActivity.startActivity(
                            this,
                            st.id,
                            deploymentId,
                            st.project?.id ?: -1,
                            Device.AUDIOMOTH.value,
                            DEPLOYMENT_REQUEST_CODE
                        )
                    }
                }
            }
        }
    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl(this)),
                CoreApiHelper(CoreApiServiceImpl(this)),
                LocalDataHelper()
            )
        ).get(DeploymentDetailViewModel::class.java)
    }

    private fun confirmationDialog() {
        val builder = MaterialAlertDialogBuilder(this, R.style.BaseAlertDialog)
        builder.setTitle(getString(R.string.delete_location))
        builder.setMessage(getString(R.string.are_you_sure_delete_location))

        builder.setPositiveButton(getString(R.string.delete)) { _, _ ->
            onDeleteLocation()
            analytics.trackDeleteDeploymentEvent(StatusEvent.SUCCESS.id)
        }
        builder.setNegativeButton(getString(R.string.back)) { _, _ -> }

        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
    }

    private fun onDeleteLocation() {
        deployment?.let {
            viewModel.deleteDeploymentLocation(
                it.id,
                object : DatabaseCallback {
                    override fun onSuccess() {
                        DeploymentSyncWorker.enqueue(this@DeploymentDetailActivity)
                        firebaseCrashlytics.setCustomKey(
                            CrashlyticsKey.DeleteLocation.key,
                            it.serverId ?: ""
                        )
                        finish()
                    }

                    override fun onFailure(errorMessage: String) {
                        showCommonDialog(errorMessage)
                    }
                }
            )
        }
    }

    private fun setupClickListener() {
        deploymentImageAdapter.onImageAdapterClickListener = object : OnImageAdapterClickListener {
            override fun onAddImageClick() {
                if (!cameraPermissions.allowed() || !galleryPermissions.allowed()) {
                    imageFile = null
                    if (!cameraPermissions.allowed()) cameraPermissions.check { }
                    if (!galleryPermissions.allowed()) galleryPermissions.check { }
                } else {
                    AddImageActivity.startActivity(
                        this@DeploymentDetailActivity,
                        deployment?.device!!,
                        deployment?.id!!,
                        newImages
                    )
                    toAddImage = true
                    finish()
                }
            }

            override fun onImageClick(deploymentImageView: DeploymentImageView) {
                val list = (
                    deploymentImages.map {
                        if (it.remotePath != null) BuildConfig.DEVICE_API_DOMAIN + it.remotePath else "file://${it.localPath}"
                    } + deploymentImageAdapter.getNewAttachImage().map { "file://$it" }
                    ) as ArrayList

                val labelList = (
                    deploymentImages.map { it.imageLabel } + deploymentImageAdapter.getNewAttachImageTyped()
                        .map { it.label }
                    ) as ArrayList
                val selectedImage =
                    deploymentImageView.remotePath ?: "file://${deploymentImageView.localPath}"
                val index = list.indexOf(selectedImage)
                val selectedLabel = labelList[index]
                list.removeAt(index)
                labelList.removeAt(index)
                list.add(0, selectedImage)
                labelList.add(0, selectedLabel)
                firebaseCrashlytics.setCustomKey(CrashlyticsKey.OnClickImage.key, selectedImage)
                DisplayImageActivity.startActivity(
                    this@DeploymentDetailActivity,
                    list.toTypedArray(),
                    labelList.toTypedArray()
                )
            }

            override fun onDeleteImageClick(position: Int, imagePath: String) {
                firebaseCrashlytics.setCustomKey(CrashlyticsKey.OnDeleteImage.key, imagePath)
                deploymentImageAdapter.removeAt(position)
            }
        }

        deploymentImageAdapter.setImages(arrayListOf())
    }

    private fun updateDeploymentDetailView(deployment: Deployment) {
        // setup deployment images view
        observeDeploymentImage(deployment.id, deployment.device ?: Device.GUARDIAN.value)
        val location = deployment.stream
        location?.let { locate ->
            latitudeValue.text = locate.latitude.latitudeCoordinates(this)
            longitudeValue.text = locate.longitude.longitudeCoordinates(this)
            altitudeValue.text = locate.altitude.setFormatLabel()
            deploymentIdTextView.text = deployment.deploymentKey
        }
    }

    private fun downloadPhotos(deploymentServerId: String?) {
        if (deploymentServerId != null) {
            DownloadImagesWorker.Companion.enqueue(this, deploymentServerId)
        }
    }

    private fun observeDeploymentImage(deploymentId: Int, device: String) {
        deployImageLiveData =
            Transformations.map(viewModel.getAllResultsAsync(deploymentId, device).asLiveData()) {
                it
            }
        deployImageLiveData.observeForever(deploymentImageObserve)
    }

    private fun updateDeploymentImages(deploymentImages: List<DeploymentImage>) {
        val items = deploymentImages.map { it.toDeploymentImageView() }
        val newImages =
            this.newImages?.map { DeploymentImageView(0, it.path!!, it.remotePath, it.name) }
        val combined = if (newImages != null) newImages + items else items
        deploymentImageAdapter.setImages(combined)
    }

    private fun setupImageRecycler() {
        deploymentImageRecycler.apply {
            adapter = deploymentImageAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
        deploymentImageAdapter.setImages(arrayListOf())
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map?.uiSettings?.setAllGesturesEnabled(false)
        moveCamera()
    }

    private fun moveCamera() {
        if (map == null) return
        val latlng = LatLng(deployment?.stream?.latitude ?: 0.0, deployment?.stream?.longitude  ?: 0.0)
        map?.moveCamera(CameraUpdateFactory.newLatLng(latlng))
        map?.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
    }
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = deployment?.stream?.name ?: getString(R.string.title_deployment_detail)
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
        deploymentLiveData.removeObserver(deploymentObserve)

        val newImages = deploymentImageAdapter.getNewAttachImageTyped()
        if (newImages.isNotEmpty() && !toAddImage) {
            viewModel.insertImage(deployment, newImages)
            ImageSyncWorker.enqueue(this)
        }
    }

    companion object {
        const val DEPLOYMENT_REQUEST_CODE = 1001
        const val DEFAULT_ZOOM = 15.0f
        const val NEW_IMAGES_EXTRA = "NEW_IMAGES_EXTRA"
        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, DeploymentDetailActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int, newImages: List<Image>) {
            val intent = Intent(context, DeploymentDetailActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            intent.putExtra(NEW_IMAGES_EXTRA, newImages as java.io.Serializable)
            context.startActivity(intent)
        }
    }
}
