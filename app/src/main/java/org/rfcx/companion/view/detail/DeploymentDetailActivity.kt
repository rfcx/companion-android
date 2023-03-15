package org.rfcx.companion.view.detail

import android.app.Activity
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.pluginscalebar.ScaleBarOptions
import com.mapbox.pluginscalebar.ScaleBarPlugin
import com.opensooq.supernova.gligar.GligarPicker
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
import java.io.File

class DeploymentDetailActivity :
    AppCompatActivity(), OnMapReadyCallback, (DeploymentImageView) -> Unit {

    private val deploymentImageAdapter by lazy { DeploymentImageAdapter() }
    private lateinit var viewModel: DeploymentDetailViewModel

    private lateinit var mapView: MapView
    private lateinit var mapBoxMap: MapboxMap
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

    private var imageFile: File? = null
    private val cameraPermissions by lazy { CameraPermissions(this) }
    private val galleryPermissions by lazy { GalleryPermissions(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_token))
        setContentView(R.layout.activity_deployment_detail)
        setViewModel()

        val preferences = Preferences.getInstance(this)
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = viewModel.getProjectById(projectId)

        deleteButton.visibility =
            if (project?.permissions == Permissions.ADMIN.value) View.VISIBLE else View.GONE

        // Setup Mapbox
        mapView = findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        deployment =
            intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
                ?.let { viewModel.getDeploymentById(it) }

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
                DeviceApiHelper(DeviceApiServiceImpl()),
                CoreApiHelper(CoreApiServiceImpl()),
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
                    startOpenGligarPicker()
                }
            }

            override fun onImageClick(deploymentImageView: DeploymentImageView) {
                val list = (
                    deploymentImages.map {
                        if (it.remotePath != null) BuildConfig.DEVICE_API_DOMAIN + it.remotePath else "file://${it.localPath}"
                    } + deploymentImageAdapter.getNewAttachImage().map { "file://$it" }
                    ) as ArrayList
                val selectedImage =
                    deploymentImageView.remotePath ?: "file://${deploymentImageView.localPath}"
                val index = list.indexOf(selectedImage)
                list.removeAt(index)
                list.add(0, selectedImage)
                firebaseCrashlytics.setCustomKey(CrashlyticsKey.OnClickImage.key, selectedImage)
                DisplayImageActivity.startActivity(
                    this@DeploymentDetailActivity,
                    list.toTypedArray()
                )
            }

            override fun onDeleteImageClick(position: Int, imagePath: String) {
                firebaseCrashlytics.setCustomKey(CrashlyticsKey.OnDeleteImage.key, imagePath)
                deploymentImageAdapter.removeAt(position)
            }
        }

        deploymentImageAdapter.setImages(arrayListOf())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        handleGligarPickerResult(requestCode, resultCode, data)

        if (requestCode == DEPLOYMENT_REQUEST_CODE) {
            forceUpdateDeployment()
        }
    }

    private fun handleTakePhotoResult(requestCode: Int, resultCode: Int) {
        if (requestCode != ImageUtils.REQUEST_TAKE_PHOTO) return

        if (resultCode == Activity.RESULT_OK) {
            imageFile?.let {
                val pathList = listOf(it.absolutePath)
                deploymentImageAdapter.addImages(pathList)
            }
        } else {
            // remove file image
            imageFile?.let {
                ImageFileUtils.removeFile(it)
                this.imageFile = null
            }
        }
    }

    private fun handleGligarPickerResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != ImageUtils.REQUEST_GALLERY || resultCode != Activity.RESULT_OK || intentData == null) return

        val pathList = mutableListOf<String>()
        val results = intentData.extras?.getStringArray(GligarPicker.IMAGES_RESULT)
        results?.forEach {
            pathList.add(it)
        }
        deploymentImageAdapter.addImages(pathList)
    }

    private fun forceUpdateDeployment() {
        if (this.deployment != null) {
            this.deployment = viewModel.getDeploymentById(this.deployment!!.id)
            this.deployment?.let { it1 ->
                updateDeploymentDetailView(it1)
                setLocationOnMap(it1)
            }

            supportActionBar?.apply {
                title = deployment?.stream?.name ?: getString(R.string.title_deployment_detail)
            }
        }
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
        deploymentImageAdapter.setImages(items)
    }

    private fun setupImageRecycler() {
        deploymentImageRecycler.apply {
            adapter = deploymentImageAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
        deploymentImageAdapter.setImages(arrayListOf())
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapBoxMap = mapboxMap
        mapboxMap.uiSettings.apply {
            setAllGesturesEnabled(false)
            isAttributionEnabled = false
            isLogoEnabled = false
        }

        mapboxMap.setStyle(Style.OUTDOORS) {
            setupScale()
            deployment?.let { it1 -> setLocationOnMap(it1) }
        }
    }

    private fun setupScale() {
        val scaleBarPlugin = ScaleBarPlugin(mapView, mapBoxMap)
        scaleBarPlugin.create(ScaleBarOptions(this))
    }

    private fun setLocationOnMap(deployment: Deployment) {
        val location = deployment.stream
        location?.let { locate ->
            val latLng = LatLng(locate.latitude, locate.longitude)
            moveCamera(latLng, DEFAULT_ZOOM)
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
        val newImages = deploymentImageAdapter.getNewAttachImage()
        if (newImages.isNotEmpty()) {
            viewModel.insertImage(deployment, newImages)
            ImageSyncWorker.enqueue(this)
        }
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

    private fun startOpenGligarPicker() {
        val remainingImage =
            DeploymentImageAdapter.MAX_IMAGE_SIZE - deploymentImageAdapter.getImageCount()
        GligarPicker()
            .requestCode(ImageUtils.REQUEST_GALLERY)
            .limit(remainingImage)
            .withActivity(this)
            .show()
    }

    companion object {
        const val DEPLOYMENT_REQUEST_CODE = 1001
        const val DEFAULT_ZOOM = 15.0

        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, DeploymentDetailActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }

    override fun invoke(deploymentImage: DeploymentImageView) {
        val list = deploymentImages.map {
            if (it.remotePath != null) BuildConfig.DEVICE_API_DOMAIN + it.remotePath else "file://${it.localPath}"
        } as ArrayList

        val index =
            list.indexOf(deploymentImage.remotePath ?: "file://${deploymentImage.localPath}")
        list.removeAt(index)
        list.add(0, deploymentImage.remotePath ?: "file://${deploymentImage.localPath}")

        DisplayImageActivity.startActivity(this, list.toTypedArray())
    }
}
