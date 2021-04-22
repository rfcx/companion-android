package org.rfcx.companion.view.detail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.pluginscalebar.ScaleBarOptions
import com.mapbox.pluginscalebar.ScaleBarPlugin
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_deployment_detail.*
import kotlinx.android.synthetic.main.buttom_sheet_attach_image_layout.view.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.BuildConfig
import org.rfcx.companion.R
import org.rfcx.companion.entity.*
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.view.BaseActivity
import org.rfcx.companion.view.deployment.EdgeDeploymentActivity.Companion.EXTRA_DEPLOYMENT_ID
import org.rfcx.companion.view.deployment.locate.LocationFragment
import java.io.File
import androidx.core.content.FileProvider
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import org.rfcx.companion.service.images.ImageSyncWorker

class DeploymentDetailActivity : BaseActivity(), OnMapReadyCallback, (DeploymentImageView) -> Unit {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val deploymentImageAdapter by lazy { DeploymentImageAdapter() }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }

    private lateinit var mapView: MapView
    private lateinit var mapBoxMap: MapboxMap
    private val analytics by lazy { Analytics(this) }

    // data
    private var deployment: EdgeDeployment? = null
    private lateinit var deployImageLiveData: LiveData<List<DeploymentImage>>
    private var deploymentImages = listOf<DeploymentImage>()
    private val deploymentImageObserve = Observer<List<DeploymentImage>> {
        deploymentImages = it
        updateDeploymentImages(deploymentImages)
    }

    private var imageFile: File? = null
    private lateinit var attachImageDialog: BottomSheetDialog
    private val cameraPermissions by lazy { CameraPermissions(this) }
    private val galleryPermissions by lazy { GalleryPermissions(this) }

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

        setupAttachImageDialog()
        setupClickListener()

        // setup onclick
        deleteButton.setOnClickListener {
            confirmationDialog()
        }

        editButton.setOnClickListener {
            deployment?.let {
                val location = deployment?.stream
                location?.let { locate ->
                    val group = locate.project?.name ?: getString(R.string.none)
                    val isGroupExisted = locationGroupDb.isExisted(locate.project?.name)
                    intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)?.let { deploymentId ->
                        analytics.trackEditLocationEvent()
                        EditLocationActivity.startActivity(
                            this,
                            locate.latitude,
                            locate.longitude,
                            locate.altitude,
                            locate.name,
                            deploymentId,
                            if (isGroupExisted) group else getString(R.string.none),
                            Device.AUDIOMOTH.value,
                            DEPLOYMENT_REQUEST_CODE
                        )
                    }
                }
            }
        }
    }

    private fun confirmationDialog() {
        val builder = AlertDialog.Builder(this, R.style.DialogCustom)
        builder.setTitle(getString(R.string.delete_location))
        builder.setMessage(getString(R.string.are_you_sure_delete_location))

        builder.setPositiveButton(getString(R.string.delete)) { _, _ ->
            onDeleteLocation()
            analytics.trackDeleteDeploymentEvent(Status.SUCCESS.id)
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

    private fun setupClickListener() {
        deploymentImageAdapter.onImageAdapterClickListener = object : OnImageAdapterClickListener {
            override fun onAddImageClick() {
                attachImageDialog.show()
            }

            override fun onImageClick(deploymentImageView: DeploymentImageView) {
                val list = deploymentImages.map {
                    if (it.remotePath != null) BuildConfig.DEVICE_API_DOMAIN + it.remotePath else "file://${it.localPath}"
                } as ArrayList

                val index = list.indexOf(deploymentImageView.remotePath ?: "file://${deploymentImageView.localPath}")
                list.removeAt(index)
                list.add(0, deploymentImageView.remotePath ?: "file://${deploymentImageView.localPath}")

                DisplayImageActivity.startActivity(this@DeploymentDetailActivity, list)
            }

            override fun onDeleteImageClick(position: Int, imagePath: String) {
                deploymentImageAdapter.removeAt(position)
                dismissImagePickerOptionsDialog()
            }
        }

        deploymentImageAdapter.setImages(arrayListOf())
        dismissImagePickerOptionsDialog()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        handleTakePhotoResult(requestCode, resultCode)
        handleGalleryResult(requestCode, resultCode, data)

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
            dismissImagePickerOptionsDialog()

        } else {
            // remove file image
            imageFile?.let {
                ImageFileUtils.removeFile(it)
                this.imageFile = null
            }
        }
    }

    private fun handleGalleryResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != ImageUtils.REQUEST_GALLERY || resultCode != Activity.RESULT_OK || intentData == null) return

        val pathList = mutableListOf<String>()
        val results = Matisse.obtainResult(intentData)
        results.forEach {
            val imagePath = ImageFileUtils.findRealPath(this, it)
            imagePath?.let { path ->
                pathList.add(path)
            }
        }
        deploymentImageAdapter.addImages(pathList)
        dismissImagePickerOptionsDialog()
    }

    private fun dismissImagePickerOptionsDialog() {
        attachImageDialog.dismiss()
    }

    private fun forceUpdateDeployment() {
        if (this.deployment != null) {
            this.deployment = edgeDeploymentDb.getDeploymentById(this.deployment!!.id)
            this.deployment?.let { it1 ->
                updateDeploymentDetailView(it1)
                setLocationOnMap(it1)
            }

            supportActionBar?.apply {
                title = deployment?.stream?.name ?: getString(R.string.title_deployment_detail)
            }
        }
    }

    private fun updateDeploymentDetailView(deployment: EdgeDeployment) {
        // setup deployment images view
        observeDeploymentImage(deployment.id)
        val location = deployment.stream
        location?.let { locate ->
            latitudeValue.text = locate.latitude.latitudeCoordinates(this)
            longitudeValue.text = locate.longitude.longitudeCoordinates(this)
            altitudeValue.text = locate.altitude.setFormatLabel()
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

    private fun setLocationOnMap(deployment: EdgeDeployment) {
        val location = deployment.stream
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
        if(newImages.isNotEmpty()) {
            deploymentImageDb.insertImage(deployment, null, newImages)
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

    private fun setupAttachImageDialog() {
        val bottomSheetView = layoutInflater.inflate(R.layout.buttom_sheet_attach_image_layout, null)

        bottomSheetView.menuGallery.setOnClickListener {
            openGallery()
        }

        bottomSheetView.menuTakePhoto.setOnClickListener {
            takePhoto()
        }

        attachImageDialog = BottomSheetDialog(this)
        attachImageDialog.setContentView(bottomSheetView)
    }

    private fun takePhoto() {
        if (!cameraPermissions.allowed()) {
            imageFile = null
            cameraPermissions.check { }
        } else {
            startTakePhoto()
        }
    }

    private fun startTakePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageFile = ImageUtils.createImageFile()
        if (imageFile != null) {
            val photoURI = FileProvider.getUriForFile(this, ImageUtils.FILE_CONTENT_PROVIDER, imageFile!!)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, ImageUtils.REQUEST_TAKE_PHOTO)
        }
    }

    private fun openGallery() {
        if (!galleryPermissions.allowed()) {
            imageFile = null
            galleryPermissions.check { }
        } else {
            startOpenGallery()
        }
    }

    private fun startOpenGallery() {
        val remainingImage = DeploymentImageAdapter.MAX_IMAGE_SIZE - deploymentImageAdapter.getImageCount()
        Matisse.from(this)
            .choose(MimeType.ofImage())
            .countable(true)
            .maxSelectable(remainingImage)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .thumbnailScale(0.85f)
            .imageEngine(GlideV4ImageEngine())
            .theme(R.style.Matisse_Dracula)
            .forResult(ImageUtils.REQUEST_GALLERY)
    }

    companion object {
        const val DEPLOYMENT_REQUEST_CODE = 1001

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
        
        val index = list.indexOf(deploymentImage.remotePath ?: "file://${deploymentImage.localPath}")
        list.removeAt(index)
        list.add(0, deploymentImage.remotePath ?: "file://${deploymentImage.localPath}")

        DisplayImageActivity.startActivity(this, list)
    }
}
