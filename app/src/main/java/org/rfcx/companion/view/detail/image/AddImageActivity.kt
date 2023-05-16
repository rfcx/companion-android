package org.rfcx.companion.view.detail.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.companion.BuildConfig
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.Device
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.CameraPermissions
import org.rfcx.companion.util.GalleryPermissions
import org.rfcx.companion.util.ImageFileUtils
import org.rfcx.companion.util.ImageUtils
import org.rfcx.companion.view.deployment.Image
import org.rfcx.companion.view.deployment.ImageAdapter
import org.rfcx.companion.view.deployment.ImageClickListener
import org.rfcx.companion.view.detail.DeploymentDetailActivity
import org.rfcx.companion.view.detail.DisplayImageActivity
import org.rfcx.companion.view.dialog.GuidelineButtonClickListener
import org.rfcx.companion.view.dialog.PhotoGuidelineDialogFragment
import java.io.File
import java.io.Serializable

class AddImageActivity : AppCompatActivity(), ImageClickListener, GuidelineButtonClickListener {

    private lateinit var viewModel: AddImageViewModel

    private var imageAdapter: ImageAdapter? = null
    private var filePath: String? = null

    private val cameraPermissions by lazy { CameraPermissions(this) }
    private val galleryPermissions by lazy { GalleryPermissions(this) }

    private val CAMERA_IMAGE_PATH = "cameraImagePath"

    private var imagePlaceHolders = listOf<String>()
    private var imageGuidelineTexts = listOf<String>()
    private var imageExamples = listOf<String>()

    private var device: String? = ""
    private var deploymentId: Int? = -1
    private var newImages: List<Image>? = null
    private var maxImages = 10

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleTakePhotoResult(requestCode, resultCode)
        handleChooseImage(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (filePath != null) {
            outState.putString(CAMERA_IMAGE_PATH, filePath.toString())
        }
    }

    private fun initIntent() {
        device = intent?.getStringExtra(DEVICE_EXTRA)
        deploymentId = intent?.getIntExtra(DEPLOYMENT_ID_EXTRA, -1)
        newImages = intent?.getSerializableExtra(NEW_IMAGES_EXTRA)?.let {
            it as List<Image>
        }
    }

    private fun getPlaceHolder() {
        when (device) {
            Device.AUDIOMOTH.value -> {
                imagePlaceHolders =
                    resources.getStringArray(R.array.audiomoth_placeholders).toList()
                imageGuidelineTexts =
                    resources.getStringArray(R.array.audiomoth_guideline_texts).toList()
                imageExamples =
                    resources.getStringArray(R.array.audiomoth_photos).toList()
                maxImages =
                    if (imagePlaceHolders.size <= 10) maxImages + (10 - maxImages) else imagePlaceHolders.size
            }
            Device.SONGMETER.value -> {
                imagePlaceHolders =
                    resources.getStringArray(R.array.songmeter_placeholders).toList()
                imageGuidelineTexts =
                    resources.getStringArray(R.array.songmeter_guideline_texts).toList()
                imageExamples =
                    resources.getStringArray(R.array.audiomoth_photos).toList()
                maxImages =
                    if (imagePlaceHolders.size <= 10) maxImages + (10 - maxImages) else imagePlaceHolders.size
            }
            Device.GUARDIAN.value + "-cell" -> {
                imagePlaceHolders =
                    resources.getStringArray(R.array.cell_guardian_placeholders)
                        .toList()
                imageGuidelineTexts =
                    resources.getStringArray(R.array.cell_guardian_guideline_texts)
                        .toList()
                imageExamples =
                    resources.getStringArray(R.array.cell_guardian_photos).toList()
                maxImages =
                    if (imagePlaceHolders.size <= 10) maxImages + (10 - maxImages) else imagePlaceHolders.size
            }
            Device.GUARDIAN.value + "-sat" -> {
                imagePlaceHolders =
                    resources.getStringArray(R.array.sat_guardian_placeholders)
                        .toList()
                imageGuidelineTexts =
                    resources.getStringArray(R.array.sat_guardian_guideline_texts)
                        .toList()
                imageExamples =
                    resources.getStringArray(R.array.sat_guardian_photos).toList()
                maxImages =
                    if (imagePlaceHolders.size <= 10) maxImages + (10 - maxImages) else imagePlaceHolders.size
            }
        }
        getImageAdapter().setMaxImages(maxImages)
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
        ).get(AddImageViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            if (it.containsKey(CAMERA_IMAGE_PATH)) {
                filePath = it.getString(CAMERA_IMAGE_PATH)
            }
        }

        setContentView(R.layout.fragment_deploy)
        setViewModel()
        initIntent()

        if (device == "guardian") {
            val types = arrayOf("cell", "sat")
            var index = 0
            MaterialAlertDialogBuilder(this)
                .setTitle("Type of guardian")
                .setSingleChoiceItems(types, index) { _, which ->
                    index = which
                }
                .setPositiveButton("Ok") { _, _ ->
                    device += "-${types[index]}"
                    getPlaceHolder()
                    setupImages()
                    setupImageRecycler()
                    updatePhotoTakenNumber()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    finish()
                }
                .show()
        } else {
            getPlaceHolder()
            setupImages()
            setupImageRecycler()
            updatePhotoTakenNumber()
        }

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
        val savedImages = viewModel.getImages(deploymentId)
        getImageAdapter().setPlaceHolders(imagePlaceHolders)
        if (savedImages.isNotEmpty()) {
            getImageAdapter().updateImagesFromSavedImages(savedImages)
        }
        if (!newImages.isNullOrEmpty()) {
            getImageAdapter().updateImagesFromSavedImages(newImages!!)
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

    private fun handleChooseImage(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != ImageUtils.REQUEST_GALLERY || resultCode != Activity.RESULT_OK || intentData == null) return

        intentData.data?.also {
            val path = ImageUtils.createImageFile(it, this)
            if (path != null) {
                getImageAdapter().updateTakeOrChooseImage(path)
            }
        }
        updatePhotoTakenNumber()
    }

    private fun setCacheImages() {
        val images = getImageAdapter().getCurrentNewImages()
        DeploymentDetailActivity.startActivity(this, deploymentId!!, images)
    }

    private fun updatePhotoTakenNumber() {
        photoTakenTextView.visibility = View.VISIBLE
        val number = getImageAdapter().getExistingImages().size
        photoTakenTextView.text =
            getString(R.string.photo_taken, number, getImageAdapter().itemCount)
    }

    override fun onPlaceHolderClick(position: Int) {
        showGuidelineDialog(position)
    }

    override fun onImageClick(image: Image) {
        val path =
            if (image.remotePath != null) BuildConfig.DEVICE_API_DOMAIN + image.remotePath else "file://${image.path}"
        DisplayImageActivity.startActivity(this, arrayOf(path), arrayOf(image.name))
    }

    override fun onDeleteClick(image: Image) {
        getImageAdapter().removeImage(image)
        updatePhotoTakenNumber()
    }

    override fun onTakePhotoClick() {
        openTakePhoto()
    }

    override fun onChoosePhotoClick() {
        openPhotoPicker()
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

    private fun openPhotoPicker() {
        if (checkPermission()) startOpenPhotoPicker()
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

    private fun startOpenPhotoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType("image/*")
            .addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, ImageUtils.REQUEST_GALLERY)
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
        finish()
    }

    override fun onBackPressed() {
        handleNextStep()
    }

    companion object {

        const val DEVICE_EXTRA = "DEVICE_EXTRA"
        const val DEPLOYMENT_ID_EXTRA = "DEPLOYMENT_ID_EXTRA"
        const val NEW_IMAGES_EXTRA = "NEW_IMAGES_EXTRA"

        fun startActivity(
            context: Context,
            device: String,
            deploymentId: Int,
            newImages: List<Image>?
        ) {
            val intent = Intent(context, AddImageActivity::class.java)
            intent.putExtra(DEVICE_EXTRA, device)
            intent.putExtra(DEPLOYMENT_ID_EXTRA, deploymentId)
            intent.putExtra(NEW_IMAGES_EXTRA, newImages as? Serializable)
            context.startActivity(intent)
        }
    }
}
