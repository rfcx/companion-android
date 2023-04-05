package org.rfcx.companion.view.detail.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.opensooq.supernova.gligar.GligarPicker
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.*
import org.rfcx.companion.util.prefs.GuardianPlan
import org.rfcx.companion.view.deployment.AudioMothDeploymentActivity
import org.rfcx.companion.view.deployment.Image
import org.rfcx.companion.view.deployment.ImageAdapter
import org.rfcx.companion.view.deployment.ImageClickListener
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol
import org.rfcx.companion.view.detail.DeploymentDetailActivity
import org.rfcx.companion.view.detail.DisplayImageActivity
import org.rfcx.companion.view.dialog.GuidelineButtonClickListener
import org.rfcx.companion.view.dialog.PhotoGuidelineDialogFragment
import java.io.File

class AddImageActivity : AppCompatActivity(), ImageClickListener, GuidelineButtonClickListener {

    private val analytics by lazy { Analytics(this) }
    private var screen: String? = null

    private var imageAdapter: ImageAdapter? = null
    private var filePath: String? = null

    private val cameraPermissions by lazy { CameraPermissions(this) }
    private val galleryPermissions by lazy { GalleryPermissions(this) }

    private val CAMERA_IMAGE_PATH = "cameraImagePath"

    private var imagePlaceHolders = listOf<String>()
    private var imageGuidelineTexts = listOf<String>()
    private var imageExamples = listOf<String>()

    private var device: String? = ""
    private var deploymentId: String? = ""

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleTakePhotoResult(requestCode, resultCode)
        handleGligarPickerResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (filePath != null) {
            outState.putString(CAMERA_IMAGE_PATH, filePath.toString())
        }
    }

    private fun initIntent() {
        device = intent?.getStringExtra(DEVICE_EXTRA)
        deploymentId = intent?.getStringExtra(DEPLOYMENT_ID_EXTRA)
    }

    private fun getPlaceHolder() {
        when (device) {
            Device.GUARDIAN.value -> {
                imagePlaceHolders =
                    resources.getStringArray(R.array.audiomoth_placeholders).toList()
                imageGuidelineTexts =
                    resources.getStringArray(R.array.audiomoth_guideline_texts).toList()
                imageExamples =
                    resources.getStringArray(R.array.audiomoth_photos).toList()
            }
            Device.SONGMETER.value -> {
                imagePlaceHolders =
                    resources.getStringArray(R.array.songmeter_placeholders).toList()
                imageGuidelineTexts =
                    resources.getStringArray(R.array.songmeter_guideline_texts).toList()
                imageExamples =
                    resources.getStringArray(R.array.audiomoth_photos).toList()
            }
            Device.SONGMETER.value + "-cell" -> {
                imagePlaceHolders =
                    resources.getStringArray(R.array.cell_guardian_placeholders)
                        .toList()
                imageGuidelineTexts =
                    resources.getStringArray(R.array.cell_guardian_guideline_texts)
                        .toList()
                imageExamples =
                    resources.getStringArray(R.array.cell_guardian_photos).toList()
            }
            Device.SONGMETER.value + "-sat" -> {
                imagePlaceHolders =
                    resources.getStringArray(R.array.sat_guardian_placeholders)
                        .toList()
                imageGuidelineTexts =
                    resources.getStringArray(R.array.sat_guardian_guideline_texts)
                        .toList()
                imageExamples =
                    resources.getStringArray(R.array.sat_guardian_photos).toList()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            if (it.containsKey(CAMERA_IMAGE_PATH)) {
                filePath = it.getString(CAMERA_IMAGE_PATH)
            }
        }

        setContentView(R.layout.fragment_deploy)
        initIntent()
        getPlaceHolder()
        setupImages()

        setupImageRecycler()
        updatePhotoTakenNumber()

        finishButton.setOnClickListener {
            val missing = getImageAdapter().getMissingImages()
            if (missing.isEmpty()) {
                handleNextStep()
            } else {
                showFinishDialog(missing)
            }
        }
    }

    private fun getImageAdapter(): ImageAdapter {
        if (imageAdapter != null) {
            return imageAdapter!!
        }
        imageAdapter = ImageAdapter(this, imageExamples)
        return imageAdapter!!
    }

    private fun setupImages() {
        val savedImages =
            audioMothDeploymentProtocol?.getImages() ?: songMeterDeploymentProtocol?.getImages()
                ?: guardianDeploymentProtocol?.getImages()
        if (savedImages != null && savedImages.isNotEmpty()) {
            getImageAdapter().updateImagesFromSavedImages(savedImages)
        } else {
            getImageAdapter().setPlaceHolders(imagePlaceHolders)
        }
    }

    private fun setupImageRecycler() {
        attachImageRecycler.apply {
            adapter = getImageAdapter()
            layoutManager = GridLayoutManager(context, 3)
        }
    }

    private fun handleTakePhotoResult(requestCode: Int, resultCode: Int) {
        if (requestCode != ImageUtils.REQUEST_TAKE_PHOTO) return

        if (resultCode == Activity.RESULT_OK) {
            filePath?.let {
                getImageAdapter().updateTakeOrChooseImage(it)
                updatePhotoTakenNumber()
            }
        } else {
            // remove file image
            filePath?.let {
                ImageFileUtils.removeFile(File(it))
                this.filePath = null
            }
        }
    }

    private fun handleGligarPickerResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != ImageUtils.REQUEST_GALLERY || resultCode != Activity.RESULT_OK || intentData == null) return

        val results = intentData.extras?.getStringArray(GligarPicker.IMAGES_RESULT)
        results?.forEach {
            getImageAdapter().updateTakeOrChooseImage(it)
        }
        updatePhotoTakenNumber()
    }

    private fun setCacheImages() {
        val images = getImageAdapter().getCurrentImagePaths()
        // save to db
    }

    private fun updatePhotoTakenNumber() {
        val number = getImageAdapter().getExistingImages().size
        photoTakenTextView.text =
            getString(R.string.photo_taken, number, getImageAdapter().itemCount)
    }

    override fun onPlaceHolderClick(position: Int) {
        showGuidelineDialog(position)
    }

    override fun onImageClick(image: Image) {
        if (image.path == null) return
        DisplayImageActivity.startActivity(this, arrayOf("file://${image.path}"), arrayOf(image.name))
    }

    override fun onDeleteClick(image: Image) {
        getImageAdapter().removeImage(image)
        updatePhotoTakenNumber()
    }

    override fun onTakePhotoClick() {
        openTakePhoto()
    }

    override fun onChoosePhotoClick() {
        openGligarPicker()
    }

    private fun startTakePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val imageFile = ImageUtils.createImageFile()
        filePath = imageFile.absolutePath
        val imageUri = FileProvider.getUriForFile(
            this,
            ImageUtils.FILE_CONTENT_PROVIDER,
            imageFile
        )
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(takePictureIntent, ImageUtils.REQUEST_TAKE_PHOTO)
    }

    private fun openTakePhoto() {
        if (checkPermission()) startTakePhoto()
    }

    private fun openGligarPicker() {
        if (checkPermission()) startOpenGligarPicker()
    }

    private fun checkPermission(): Boolean {
        return if (!cameraPermissions.allowed() || !galleryPermissions.allowed()) {
            filePath = null
            if (!cameraPermissions.allowed()) cameraPermissions.check { }
            if (!galleryPermissions.allowed()) galleryPermissions.check { }
            false
        } else {
            true
        }
    }

    private fun startOpenGligarPicker() {
        GligarPicker()
            .requestCode(ImageUtils.REQUEST_GALLERY)
            .limit(1)
            .withActivity(this)
            .disableCamera(true)
            .show()
    }

    private fun showGuidelineDialog(position: Int) {
        val guidelineDialog: PhotoGuidelineDialogFragment =
            this.supportFragmentManager.findFragmentByTag(PhotoGuidelineDialogFragment::class.java.name) as PhotoGuidelineDialogFragment?
                ?: run {
                    PhotoGuidelineDialogFragment.newInstance(
                        this,
                        imageGuidelineTexts.getOrNull(position)
                            ?: getString(R.string.take_other),
                        imageExamples.getOrNull(position) ?: "other"
                    )
                }
        if (guidelineDialog.isVisible || guidelineDialog.isAdded) return
        guidelineDialog.show(
            this.supportFragmentManager,
            PhotoGuidelineDialogFragment::class.java.name
        )
    }

    private fun showFinishDialog(missing: List<Image>) {
        MaterialAlertDialogBuilder(this, R.style.BaseAlertDialog).apply {
            setTitle(context.getString(R.string.missing_dialog_title))
            setMessage(
                context.getString(
                    R.string.follow_missing, missing.joinToString("\n") { "${it.id}. ${it.name}" }
                )
            )
            setPositiveButton(R.string.back) { _, _ -> }
            setNegativeButton(R.string.button_continue) { _, _ ->
                handleNextStep()
            }
        }.create().show()
    }

    private fun handleNextStep() {
        setCacheImages()

    }

    companion object {

        const val DEVICE_EXTRA = "DEVICE_EXTRA"
        const val DEPLOYMENT_ID_EXTRA = "DEPLOYMENT_ID_EXTRA"

        fun startActivity(context: Context, device: String, deploymentId: String) {
            val intent = Intent(context, AddImageActivity::class.java)
            intent.putExtra(DEVICE_EXTRA, device)
            intent.putExtra(DEPLOYMENT_ID_EXTRA, deploymentId)
            context.startActivity(intent)
        }
    }
}
