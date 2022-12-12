package org.rfcx.companion.view.deployment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.opensooq.supernova.gligar.GligarPicker
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.*
import org.rfcx.companion.util.prefs.GuardianPlan
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol
import org.rfcx.companion.view.detail.DisplayImageActivity
import org.rfcx.companion.view.dialog.GuidelineButtonClickListener
import org.rfcx.companion.view.dialog.PhotoGuidelineDialogFragment
import java.io.File

class DeployFragment : Fragment(), ImageClickListener, GuidelineButtonClickListener {

    private val analytics by lazy { context?.let { Analytics(it) } }
    private var screen: String? = null

    private var imageAdapter: ImageAdapter? = null
    private var filePath: String? = null

    private val cameraPermissions by lazy { CameraPermissions(context as Activity) }
    private val galleryPermissions by lazy { GalleryPermissions(context as Activity) }

    private val CAMERA_IMAGE_PATH = "cameraImagePath"

    private var imagePlaceHolders = listOf<String>()
    private var imageGuidelineTexts = listOf<String>()
    private var imageExamples = listOf<String>()

    private var audioMothDeploymentProtocol: BaseDeploymentProtocol? = null
    private var songMeterDeploymentProtocol: BaseDeploymentProtocol? = null
    private var guardianDeploymentProtocol: GuardianDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        when (context) {
            is AudioMothDeploymentProtocol -> {
                audioMothDeploymentProtocol = context
                audioMothDeploymentProtocol?.let {
                    it.showToolbar()
                    it.setCurrentPage(requireContext().resources.getStringArray(R.array.edge_optional_checks)[0])
                    it.setToolbarTitle()
                }
                imagePlaceHolders =
                    context.resources.getStringArray(R.array.audiomoth_placeholders).toList()
                imageGuidelineTexts =
                    context.resources.getStringArray(R.array.audiomoth_guideline_texts).toList()
                imageExamples =
                    context.resources.getStringArray(R.array.audiomoth_photos).toList()
            }
            is SongMeterDeploymentProtocol -> {
                songMeterDeploymentProtocol = context
                songMeterDeploymentProtocol?.let {
                    it.showToolbar()
                    it.setCurrentPage(requireContext().resources.getStringArray(R.array.song_meter_optional_checks)[0])
                    it.setToolbarTitle()
                }
                imagePlaceHolders =
                    context.resources.getStringArray(R.array.songmeter_placeholders).toList()
                imageGuidelineTexts =
                    context.resources.getStringArray(R.array.songmeter_guideline_texts).toList()
                imageExamples =
                    context.resources.getStringArray(R.array.audiomoth_photos).toList()
            }
            is GuardianDeploymentProtocol -> {
                guardianDeploymentProtocol = context
                guardianDeploymentProtocol?.let {
                    it.showToolbar()
                    it.setCurrentPage(requireContext().resources.getStringArray(R.array.guardian_optional_checks)[0])
                    it.setToolbarTitle()
                }

                when (guardianDeploymentProtocol?.getGuardianPlan()) {
                    GuardianPlan.SAT_ONLY -> {
                        imagePlaceHolders =
                            context.resources.getStringArray(R.array.sat_guardian_placeholders)
                                .toList()
                        imageGuidelineTexts =
                            context.resources.getStringArray(R.array.sat_guardian_guideline_texts)
                                .toList()
                        imageExamples =
                            context.resources.getStringArray(R.array.sat_guardian_photos).toList()
                    }
                    else -> {
                        imagePlaceHolders =
                            context.resources.getStringArray(R.array.cell_guardian_placeholders)
                                .toList()
                        imageGuidelineTexts =
                            context.resources.getStringArray(R.array.cell_guardian_guideline_texts)
                                .toList()
                        imageExamples =
                            context.resources.getStringArray(R.array.cell_guardian_photos).toList()
                    }
                }
            }
        }
    }

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
        arguments?.let {
            screen = it.getString(ARG_SCREEN)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
        setupImages()
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deploy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            if (it.containsKey(CAMERA_IMAGE_PATH)) {
                filePath = it.getString(CAMERA_IMAGE_PATH)
            }
        }

        setupImageRecycler()
        updatePhotoTakenNumber()

        finishButton.setOnClickListener {
            setCacheImages()
            val images = getImageAdapter().getExistingImages()
            val missing = getImageAdapter().getMissingImages()
            showFinishDialog(images, missing)
        }
    }

    private fun handleTakePhotoResult(requestCode: Int, resultCode: Int) {
        if (requestCode != ImageUtils.REQUEST_TAKE_PHOTO) return

        if (resultCode == Activity.RESULT_OK) {
            filePath?.let {
                getImageAdapter().updateTakeOrChooseImage(it)
                setCacheImages()
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
        setCacheImages()
        updatePhotoTakenNumber()
    }

    private fun setCacheImages() {
        val images = getImageAdapter().getCurrentImagePaths()
        audioMothDeploymentProtocol?.setImages(images)
        songMeterDeploymentProtocol?.setImages(images)
        guardianDeploymentProtocol?.setImages(images)
    }

    private fun updatePhotoTakenNumber() {
        val number = getImageAdapter().getExistingImages().size
        photoTakenTextView.text = getString(R.string.photo_taken, number, getImageAdapter().itemCount)
    }

    override fun onPlaceHolderClick(position: Int) {
        showGuidelineDialog(position)
    }

    override fun onImageClick(image: Image) {
        if (image.path == null) return
        context?.let { DisplayImageActivity.startActivity(it, arrayOf("file://${image.path}")) }
    }

    override fun onDeleteClick(image: Image) {
        getImageAdapter().removeImage(image)
        setCacheImages()
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
        val imageUri =
            context?.let {
                FileProvider.getUriForFile(
                    it,
                    ImageUtils.FILE_CONTENT_PROVIDER,
                    imageFile
                )
            }
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
            .withFragment(this)
            .disableCamera(true)
            .show()
    }

    private fun showGuidelineDialog(position: Int) {
        val guidelineDialog: PhotoGuidelineDialogFragment =
            this.parentFragmentManager.findFragmentByTag(PhotoGuidelineDialogFragment::class.java.name) as PhotoGuidelineDialogFragment?
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
            this.parentFragmentManager,
            PhotoGuidelineDialogFragment::class.java.name
        )
    }

    private fun showFinishDialog(existing: List<Image>, missing: List<Image>) {
        val dialogBuilder: AlertDialog.Builder =
            AlertDialog.Builder(requireContext()).apply {
                setTitle("Are you sure to finish ?")
                setMessage("There are missing photos by following ${missing.map { it.name }.joinToString("\n")}")
                setPositiveButton(R.string.go_back) { _, _ ->
                }
                setNegativeButton("Skip anyway") { _, _ ->
                    handleNextStep(existing)
                }
            }
        dialogBuilder.create().show()
    }

    private fun handleNextStep(images: List<Image>) {
        when (screen) {
            Screen.AUDIO_MOTH_CHECK_LIST.id -> {
                if (images.isNotEmpty()) {
                    analytics?.trackAddDeploymentImageEvent(Device.AUDIOMOTH.value)
                }
                audioMothDeploymentProtocol?.nextStep()
            }
            Screen.SONG_METER_CHECK_LIST.id -> {
                if (images.isNotEmpty()) {
                    analytics?.trackAddDeploymentImageEvent(Device.SONGMETER.value)
                }
                songMeterDeploymentProtocol?.nextStep()
            }
            Screen.GUARDIAN_CHECK_LIST.id -> {
                if (images.isNotEmpty()) {
                    analytics?.trackAddDeploymentImageEvent(Device.GUARDIAN.value)
                }
                guardianDeploymentProtocol?.nextStep()
            }
        }
    }

    companion object {
        private const val ARG_SCREEN = "screen"

        fun newInstance(screen: String): DeployFragment {
            return DeployFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SCREEN, screen)
                }
            }
        }
    }
}
